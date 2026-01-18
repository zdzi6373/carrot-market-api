# 🛠️ OCI Ubuntu Server Setup Guide (Complete Version)

이 가이드는 Oracle Cloud Infrastructure (OCI) 의 Ubuntu 22.04 Minimal (1GB RAM) 인스턴스를 기준으로 작성되었습니다. Minimal 버전의 누락된 패키지 설치부터 메모리 최적화, WAS 구축까지의 전 과정을 다룹니다.

---

## 0. Server Spec

- **OS:** Ubuntu 22.04 LTS (Minimal Image)
- **CPU:** AMD64 (Standard E1 / Micro)
- **RAM:** 1GB (+4GB Swap 필수 설정)
- **Disk:** 47GB Default

---

## 1. System Initialization (Minimal 필수 패키지 & 최적화)

Minimal 이미지는 편집기나 방화벽 저장 도구가 없으므로 가장 먼저 수행해야 합니다.

### 1-1. 필수 유틸리티 설치

nano (편집기), wget/curl (다운로드), iptables-persistent (방화벽 저장)를 설치합니다. 설치 중 분홍색 화면이 나오면 `[Yes]`를 선택하세요.

```bash
sudo apt update
sudo apt install nano wget curl net-tools iptables iptables-persistent netfilter-persistent -y
```

### 1-2. 4GB Swap Memory 설정 (★매우 중요)

1GB 램에서 Tomcat과 DB가 동시에 구동되다가 뻗는 것을 방지하기 위해 디스크 4GB를 가상 메모리로 할당합니다.

```bash
# 1. 4GB 빈 파일 생성
sudo fallocate -l 4G /swapfile

# 2. 권한 설정 (루트만 접근 가능)
sudo chmod 600 /swapfile

# 3. 스왑 영역 포맷 및 활성화
sudo mkswap /swapfile
sudo swapon /swapfile

# 4. 재부팅 후에도 유지되도록 설정 등록
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 5. 적용 확인 (Total Swap이 4.0G 인지 확인)
free -h
```

---

## 2. Java Setup (Amazon Corretto 17)

Tomcat 10 구동을 위한 고성능 JDK를 설치합니다.

```bash
# 1. 설치 파일 다운로드 (User-Agent 헤더 추가로 차단 우회)
wget --user-agent="Mozilla/5.0" https://corretto.aws/downloads/resources/17.0.17.10.1/java-17-amazon-corretto-jdk_17.0.17.10.1-1_amd64.deb

# 2. 설치 진행
sudo apt install ./java-17-amazon-corretto-jdk_17.0.17.10.1-1_amd64.deb -y

# 3. 버전 고정 (자동 업데이트로 인한 호환성 문제 방지)
sudo apt-mark hold java-17-amazon-corretto-jdk

# 4. 설치 경로 확인 (Tomcat 설정 시 필요)
# 경로: /usr/lib/jvm/java-17-amazon-corretto-jdk
java -version
```

---

## 3. Database Setup (PostgreSQL 16)

보안을 위해 SSH Tunneling으로만 접속하도록 설정합니다.

### 3-1. 설치 (16.11 버전)

```bash
# 1. 공식 리포지토리 키 및 목록 등록
sudo mkdir -p /usr/share/postgresql-common/pgdg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo gpg --dearmor -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg > /dev/null
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt update

# 2. 특정 버전 설치 및 고정
sudo apt install postgresql-16=16.11-1.pgdg22.04+1 -y
sudo apt-mark hold postgresql-16
```

### 3-2. 접속 허용 설정

1. `sudo nano /etc/postgresql/16/main/postgresql.conf`
   - `listen_addresses = '*'` 로 수정

2. `sudo nano /etc/postgresql/16/main/pg_hba.conf`
   - `host all all 0.0.0.0/0 scram-sha-256` 줄 추가

3. 재시작: `sudo systemctl restart postgresql`

---

## 4. Web Server Setup (Tomcat 10.1.49)

apt 저장소에 없는 최신 버전을 수동 설치하고 서비스로 등록합니다.

### 4-1. 다운로드 및 설치

```bash
# 1. 다운로드 및 압축 해제 (/opt/tomcat)
cd /tmp
wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.49/bin/apache-tomcat-10.1.49.tar.gz
sudo mkdir -p /opt/tomcat
sudo tar -xzvf apache-tomcat-10.1.49.tar.gz -C /opt/tomcat --strip-components=1

# 2. 전용 사용자 생성 및 권한 부여
sudo groupadd tomcat
sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
sudo chown -R tomcat: /opt/tomcat
sudo chmod +x /opt/tomcat/bin/*.sh
```

### 4-2. Systemd 서비스 등록

`sudo nano /etc/systemd/system/tomcat.service` 파일을 생성하고 아래 내용을 입력합니다.

```ini
[Unit]
Description=Apache Tomcat 10.1.49 Web Application Container
After=network.target

[Service]
Type=forking

# ★ Java 경로 지정 (Corretto 17)
Environment="JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto-jdk"

Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_BASE=/opt/tomcat"

# ★ 1GB RAM 최적화 (Swap 믿고 너무 늘리지 말 것, 512MB 제한)
Environment="CATALINA_OPTS=-Xms256M -Xmx512M -server -XX:+UseParallelGC"
Environment="JAVA_OPTS=-Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

User=tomcat
Group=tomcat
UMask=0007
RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target
```

### 4-3. 실행 및 활성화

```bash
sudo systemctl daemon-reload
sudo systemctl start tomcat
sudo systemctl enable tomcat
```

---

## 5. Firewall Configuration (8080 Port)

Minimal 버전에서는 iptables-persistent를 통해 규칙을 영구 저장해야 재부팅 후에도 접속됩니다.

### 5-1. OCI Console (클라우드 방화벽)

- **Ingress Rules:** Protocol TCP, Port 8080, CIDR `0.0.0.0/0` 추가.

### 5-2. Ubuntu Server (내부 방화벽)

```bash
# 1. 8080 포트 개방 규칙 추가 (기존 규칙보다 상위에 삽입)
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8080 -j ACCEPT

# 2. 규칙 확인
sudo iptables -L -n -v --line-numbers | grep 8080

# 3. ★ 규칙 영구 저장 (Minimal 버전 필수 단계)
sudo netfilter-persistent save
```
