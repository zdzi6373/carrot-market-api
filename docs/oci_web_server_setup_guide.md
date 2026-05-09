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

---

## 6. Domain Setup (DuckDNS)

무료 동적 DNS 서비스인 DuckDNS를 이용해 OCI 서버 공인 IP에 도메인을 연결합니다.

### 6-1. DuckDNS 도메인 등록

1. [https://www.duckdns.org](https://www.duckdns.org) 접속 후 로그인
2. 원하는 서브도메인 입력 (예: `carrot-market`) → **add domain** 클릭
3. **current ip** 항목에 OCI 서버 공인 IP 입력 후 **update ip** 클릭
4. 토큰 값 복사 (Certbot 인증 시 필요)

### 6-2. IP 자동 갱신 스크립트 (선택사항)

OCI Free Tier는 IP가 고정이므로 필수는 아니지만, 변동 IP 환경에서는 아래 스크립트로 자동 갱신합니다.

```bash
# 스크립트 생성
mkdir -p ~/duckdns
nano ~/duckdns/duck.sh
```

```bash
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=YOUR_DOMAIN&token=YOUR_TOKEN&ip=" \
  | curl -k -o ~/duckdns/duck.log -K -
```

```bash
chmod +x ~/duckdns/duck.sh

# crontab으로 5분마다 실행
crontab -e
# 아래 줄 추가
*/5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1
```

---

## 7. Nginx Setup (Reverse Proxy)

외부 80/443 트래픽을 앞단에서 받아 Tomcat(localhost:8080)으로 전달하는 리버스 프록시를 구성합니다.

### 7-1. Nginx 설치

```bash
sudo apt update
sudo apt install nginx -y
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 7-2. 방화벽에 80/443 포트 추가

**OCI Console:**
- Ingress Rules에 TCP 80, TCP 443 추가

**Ubuntu 내부 방화벽:**
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### 7-3. Nginx 설정 파일 작성

```bash
sudo nano /etc/nginx/sites-available/carrot-market
```

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN.duckdns.org;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# 설정 활성화
sudo ln -s /etc/nginx/sites-available/carrot-market /etc/nginx/sites-enabled/
sudo nginx -t        # 문법 검사
sudo systemctl reload nginx
```

---

## 8. HTTPS Setup (Certbot + Let's Encrypt)

Certbot을 이용해 Let's Encrypt 무료 SSL 인증서를 발급받고 Nginx에 연동합니다.

### 8-1. Certbot 설치

```bash
sudo apt install certbot python3-certbot-nginx -y
```

### 8-2. 인증서 발급

```bash
sudo certbot --nginx -d YOUR_DOMAIN.duckdns.org
```

진행 중 이메일 입력 및 약관 동의 후 발급이 완료되면 Certbot이 Nginx 설정을 자동으로 수정합니다.

### 8-3. 자동 갱신 확인

Let's Encrypt 인증서는 90일마다 만료되며, Certbot이 자동 갱신 타이머를 등록합니다.

```bash
# 자동 갱신 타이머 확인
sudo systemctl status certbot.timer

# 갱신 테스트 (실제 발급 없이 시뮬레이션)
sudo certbot renew --dry-run
```

### 8-4. 최종 Nginx 설정 (Certbot 적용 후)

Certbot 적용 후 `/etc/nginx/sites-available/carrot-market` 파일은 아래와 같이 변경됩니다.

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN.duckdns.org;
    return 301 https://$host$request_uri;  # HTTP → HTTPS 리다이렉트
}

server {
    listen 443 ssl;
    server_name YOUR_DOMAIN.duckdns.org;

    ssl_certificate /etc/letsencrypt/live/YOUR_DOMAIN.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/YOUR_DOMAIN.duckdns.org/privkey.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
