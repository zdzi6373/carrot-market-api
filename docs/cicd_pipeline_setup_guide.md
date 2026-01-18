# CI/CD Pipeline Setup Guide (GitHub Actions + OCI)

이 가이드는 GitHub Actions를 사용하여 `develop` 브랜치 push 시 OCI 서버에 자동 배포하는 파이프라인 구축 과정을 다룹니다.

---

## 0. Architecture Overview

```
[GitHub Repository]
       │
       │ develop branch push
       ▼
[GitHub Actions Runner]
       │
       ├── 1. Checkout 소스코드
       ├── 2. JDK 17 설정
       ├── 3. db.properties 생성 (Secrets 주입)
       ├── 4. Maven 빌드 (WAR 생성)
       ├── 5. SCP로 WAR 전송
       └── 6. SSH로 배포 스크립트 실행
               │
               ▼
[OCI Server (Ubuntu 22.04)]
       │
       ├── /opt/tomcat/scripts/deploy.sh
       │     ├── 기존 WAR 백업
       │     ├── Tomcat 중지
       │     ├── WAR 교체
       │     └── Tomcat 시작
       │
       └── /opt/tomcat/webapps/carrot-market-api.war
```

---

## 1. OCI Server Preparation

배포 자동화를 위해 서버에 SSH 키와 스크립트를 설정합니다.

### 1-1. SSH Key 생성 (로컬 PC에서 실행)

GitHub Actions가 서버에 접속할 때 사용할 SSH 키를 생성합니다.

```bash
# ED25519 키 생성 (권장)
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy_key

# 또는 RSA 키 생성
ssh-keygen -t rsa -b 4096 -C "github-actions-deploy" -f ~/.ssh/github_deploy_key
```

### 1-2. 공개키를 OCI 서버에 등록

```bash
# OCI 서버에 SSH 접속 후 실행
# 로컬에서 생성한 공개키 내용을 복사하여 추가
echo "ssh-ed25519 AAAA... github-actions-deploy" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### 1-3. 배포 디렉토리 생성

```bash
# OCI 서버에서 실행
sudo mkdir -p /opt/tomcat/backup
sudo mkdir -p /opt/tomcat/scripts
sudo chown -R tomcat:tomcat /opt/tomcat/backup
sudo chown -R ubuntu:ubuntu /opt/tomcat/scripts
```

### 1-4. 배포 스크립트 생성

`sudo nano /opt/tomcat/scripts/deploy.sh` 파일을 생성하고 아래 내용을 입력합니다.

```bash
#!/bin/bash
set -e

TOMCAT_HOME="/opt/tomcat"
WAR_NAME="carrot-market-api.war"
APP_NAME="carrot-market-api"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "[$(date)] 배포 시작"

# 1. 기존 WAR 백업
if [ -f ${TOMCAT_HOME}/webapps/${WAR_NAME} ]; then
    sudo -u tomcat cp ${TOMCAT_HOME}/webapps/${WAR_NAME} \
        ${TOMCAT_HOME}/backup/${APP_NAME}_${TIMESTAMP}.war
    echo "[$(date)] 백업 완료: ${APP_NAME}_${TIMESTAMP}.war"
fi

# 2. Tomcat 중지
sudo systemctl stop tomcat || true
sleep 3

# 3. 기존 배포 정리
sudo rm -rf ${TOMCAT_HOME}/webapps/${APP_NAME}
sudo rm -f ${TOMCAT_HOME}/webapps/${WAR_NAME}

# 4. 새 WAR 배포
sudo mv /tmp/${WAR_NAME} ${TOMCAT_HOME}/webapps/
sudo chown tomcat:tomcat ${TOMCAT_HOME}/webapps/${WAR_NAME}

# 5. Tomcat 시작
sudo systemctl start tomcat

# 6. 오래된 백업 정리 (최근 5개만 유지)
cd ${TOMCAT_HOME}/backup
ls -t ${APP_NAME}_*.war 2>/dev/null | tail -n +6 | xargs -r sudo rm -f

echo "[$(date)] 배포 완료"
```

### 1-5. 롤백 스크립트 생성 (선택사항)

`sudo nano /opt/tomcat/scripts/rollback.sh` 파일을 생성합니다.

```bash
#!/bin/bash
set -e

TOMCAT_HOME="/opt/tomcat"
WAR_NAME="carrot-market-api.war"
APP_NAME="carrot-market-api"

# 가장 최근 백업 파일 찾기
BACKUP_FILE=$(ls -t ${TOMCAT_HOME}/backup/${APP_NAME}_*.war 2>/dev/null | head -1)

if [ -z "$BACKUP_FILE" ]; then
    echo "백업 파일이 없습니다."
    exit 1
fi

echo "[$(date)] 롤백 시작: $BACKUP_FILE"

sudo systemctl stop tomcat || true
sleep 3

sudo rm -rf ${TOMCAT_HOME}/webapps/${APP_NAME}
sudo rm -f ${TOMCAT_HOME}/webapps/${WAR_NAME}
sudo cp "$BACKUP_FILE" ${TOMCAT_HOME}/webapps/${WAR_NAME}
sudo chown tomcat:tomcat ${TOMCAT_HOME}/webapps/${WAR_NAME}

sudo systemctl start tomcat

echo "[$(date)] 롤백 완료"
```

### 1-6. 스크립트 실행 권한 부여

```bash
sudo chmod +x /opt/tomcat/scripts/deploy.sh
sudo chmod +x /opt/tomcat/scripts/rollback.sh
```

### 1-7. sudo 권한 설정 (비밀번호 없이 실행)

`sudo visudo` 명령으로 sudoers 파일을 열고 아래 내용을 추가합니다.

```bash
# GitHub Actions 배포용 sudo 권한
ubuntu ALL=(ALL) NOPASSWD: /bin/systemctl stop tomcat
ubuntu ALL=(ALL) NOPASSWD: /bin/systemctl start tomcat
ubuntu ALL=(ALL) NOPASSWD: /bin/mv /tmp/carrot-market-api.war /opt/tomcat/webapps/
ubuntu ALL=(ALL) NOPASSWD: /bin/chown tomcat\:tomcat /opt/tomcat/webapps/carrot-market-api.war
ubuntu ALL=(ALL) NOPASSWD: /bin/rm -rf /opt/tomcat/webapps/carrot-market-api
ubuntu ALL=(ALL) NOPASSWD: /bin/rm -f /opt/tomcat/webapps/carrot-market-api.war
ubuntu ALL=(ALL) NOPASSWD: /bin/rm -f /opt/tomcat/backup/*
```

---

## 2. GitHub Secrets Configuration

GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `OCI_HOST` | OCI 서버 공인 IP 주소 | `123.456.789.012` |
| `OCI_USERNAME` | SSH 접속 사용자명 | `ubuntu` |
| `OCI_SSH_PRIVATE_KEY` | SSH 개인키 전체 내용 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `DB_URL` | PostgreSQL 접속 URL | `jdbc:postgresql://localhost:5432/carrot_market` |
| `DB_USERNAME` | DB 사용자명 | `postgres` |
| `DB_PASSWORD` | DB 비밀번호 | `your_secure_password` |

### SSH 개인키 등록 방법

```bash
# 로컬에서 개인키 내용 확인
cat ~/.ssh/github_deploy_key

# 출력된 전체 내용 (-----BEGIN ~ -----END 포함)을 복사하여
# OCI_SSH_PRIVATE_KEY Secret에 등록
```

---

## 3. GitHub Actions Workflow

프로젝트 루트에 `.github/workflows/deploy.yml` 파일을 생성합니다.

```yaml
name: Build and Deploy to OCI

on:
  push:
    branches:
      - develop

env:
  WAR_NAME: carrot-market-api.war

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      # 1. 소스코드 체크아웃
      - name: Checkout code
        uses: actions/checkout@v4

      # 2. JDK 17 설정
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      # 3. 운영환경 db.properties 생성
      - name: Create db.properties
        run: |
          mkdir -p src/main/resources
          cat << EOF > src/main/resources/db.properties
          db.url=${{ secrets.DB_URL }}
          db.username=${{ secrets.DB_USERNAME }}
          db.password=${{ secrets.DB_PASSWORD }}
          db.driver=org.postgresql.Driver
          EOF

      # 4. Maven 빌드
      - name: Build with Maven
        run: mvn clean package -DskipTests

      # 5. WAR 파일 확인
      - name: Verify WAR file
        run: |
          if [ ! -f target/${{ env.WAR_NAME }} ]; then
            echo "WAR file not found!"
            exit 1
          fi
          ls -lh target/${{ env.WAR_NAME }}

      # 6. SSH 키 설정
      - name: Setup SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.OCI_SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ secrets.OCI_HOST }} >> ~/.ssh/known_hosts

      # 7. WAR 파일 전송
      - name: Upload WAR to server
        run: |
          scp target/${{ env.WAR_NAME }} \
            ${{ secrets.OCI_USERNAME }}@${{ secrets.OCI_HOST }}:/tmp/${{ env.WAR_NAME }}

      # 8. 배포 스크립트 실행
      - name: Deploy
        run: |
          ssh ${{ secrets.OCI_USERNAME }}@${{ secrets.OCI_HOST }} \
            'bash /opt/tomcat/scripts/deploy.sh'

      # 9. SSH 키 정리
      - name: Cleanup SSH key
        if: always()
        run: rm -rf ~/.ssh/id_rsa
```

---

## 4. Deployment Test

### 4-1. 수동 테스트 (로컬에서 SSH 접속 확인)

```bash
# SSH 접속 테스트
ssh -i ~/.ssh/github_deploy_key ubuntu@<OCI_HOST>

# 배포 스크립트 수동 실행 테스트
bash /opt/tomcat/scripts/deploy.sh
```

### 4-2. 자동 배포 테스트

```bash
# develop 브랜치에 커밋 후 push
git checkout develop
git add .
git commit -m "test: CI/CD 파이프라인 테스트"
git push origin develop
```

### 4-3. 배포 결과 확인

**GitHub Actions 확인:**
- GitHub 저장소 → Actions 탭 → 워크플로우 실행 상태 확인

**OCI 서버에서 확인:**
```bash
# Tomcat 로그 확인
tail -f /opt/tomcat/logs/catalina.out

# 애플리케이션 상태 확인
curl http://localhost:8080/carrot-market-api/api/products

# 백업 파일 확인
ls -lht /opt/tomcat/backup/
```

---

## 5. Rollback Procedure

배포 후 문제 발생 시 롤백을 수행합니다.

```bash
# OCI 서버에서 실행
bash /opt/tomcat/scripts/rollback.sh

# 특정 버전으로 롤백하려면 백업 파일 목록 확인 후 수동 복원
ls -lht /opt/tomcat/backup/
sudo systemctl stop tomcat
sudo cp /opt/tomcat/backup/carrot-market-api_20240115_143022.war /opt/tomcat/webapps/carrot-market-api.war
sudo chown tomcat:tomcat /opt/tomcat/webapps/carrot-market-api.war
sudo systemctl start tomcat
```

---

## 6. Troubleshooting

| 문제 | 원인 | 해결 방법 |
|------|------|----------|
| SSH 연결 실패 | 키 불일치 또는 권한 문제 | `chmod 600 ~/.ssh/id_rsa`, known_hosts 확인 |
| Permission denied | sudo 권한 부족 | visudo로 권한 추가 |
| WAR 배포 후 404 | Context path 불일치 | WAR 파일명이 `carrot-market-api.war`인지 확인 |
| DB 연결 실패 | db.properties 생성 실패 | GitHub Actions 로그에서 파일 생성 단계 확인 |
| Tomcat 시작 실패 | 메모리 부족 | `free -h`로 메모리 확인, swap 설정 확인 |
| 빌드 실패 | Maven 또는 Java 버전 문제 | `pom.xml`의 Java 버전과 Actions 설정 일치 확인 |

### 로그 확인 명령어

```bash
# Tomcat 로그
tail -100 /opt/tomcat/logs/catalina.out

# 시스템 로그
journalctl -u tomcat -n 50

# GitHub Actions 로그
# GitHub 저장소 → Actions → 해당 워크플로우 클릭 → 각 step 로그 확인
```

---

## 7. Security Considerations

1. **SSH 키 관리**
   - 개인키는 GitHub Secrets에만 저장
   - 로컬 개인키는 배포 전용으로만 사용
   - 주기적으로 키 갱신 권장

2. **DB 자격 증명**
   - `db.properties`는 절대 Git에 커밋하지 않음 (`.gitignore`에 포함)
   - 운영 DB 비밀번호는 강력한 비밀번호 사용

3. **서버 접근 제한**
   - OCI 보안 목록에서 SSH(22) 포트 접근 IP 제한 권장
   - 배포용 SSH 키는 배포 작업에만 사용

---

## 8. File Structure Summary

```
프로젝트 (GitHub)
├── .github/
│   └── workflows/
│       └── deploy.yml          # GitHub Actions 워크플로우
├── src/main/resources/
│   └── db.properties           # .gitignore (CI/CD에서 생성)
└── pom.xml

OCI 서버
├── /opt/tomcat/
│   ├── webapps/
│   │   └── carrot-market-api.war
│   ├── backup/
│   │   └── carrot-market-api_YYYYMMDD_HHMMSS.war
│   ├── scripts/
│   │   ├── deploy.sh
│   │   └── rollback.sh
│   └── logs/
│       └── catalina.out
└── ~/.ssh/
    └── authorized_keys         # GitHub Actions용 공개키
```
