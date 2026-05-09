# OCI Ubuntu Server Setup Guide (完全版)

このガイドは、Oracle Cloud Infrastructure (OCI) の Ubuntu 22.04 Minimal（1GB RAM）インスタンスを対象に作成しています。Minimalイメージに不足しているパッケージのインストールから、メモリ最適化、WAS構築までの全工程を説明します。

---

## 0. Server Spec

- **OS:** Ubuntu 22.04 LTS (Minimal Image)
- **CPU:** AMD64 (Standard E1 / Micro)
- **RAM:** 1GB (+4GB Swap 必須設定)
- **Disk:** 47GB Default

---

## 1. System Initialization (Minimal必須パッケージ & 最適化)

Minimalイメージにはエディタやファイアウォール保存ツールがないため、最初に実行します。

### 1-1. 必須ユーティリティのインストール

nano（エディタ）、wget/curl（ダウンロード）、iptables-persistent（ファイアウォール保存）をインストールします。インストール中にピンク色の画面が出たら `[Yes]` を選択してください。

```bash
sudo apt update
sudo apt install nano wget curl net-tools iptables iptables-persistent netfilter-persistent -y
```

### 1-2. 4GB Swapメモリの設定（★非常に重要）

1GB RAMでTomcatとDBを同時に起動するとクラッシュするため、ディスク4GBを仮想メモリとして割り当てます。

```bash
# 1. 4GBの空ファイルを作成
sudo fallocate -l 4G /swapfile

# 2. 権限設定（rootのみアクセス可能）
sudo chmod 600 /swapfile

# 3. スワップ領域のフォーマットと有効化
sudo mkswap /swapfile
sudo swapon /swapfile

# 4. 再起動後も維持されるよう設定登録
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 5. 適用確認（Total Swapが4.0Gになっているか確認）
free -h
```

---

## 2. Java Setup (Amazon Corretto 17)

Tomcat 10を動かすための高性能JDKをインストールします。

```bash
# 1. インストールファイルのダウンロード（User-Agentヘッダー追加でブロック回避）
wget --user-agent="Mozilla/5.0" https://corretto.aws/downloads/resources/17.0.17.10.1/java-17-amazon-corretto-jdk_17.0.17.10.1-1_amd64.deb

# 2. インストール実行
sudo apt install ./java-17-amazon-corretto-jdk_17.0.17.10.1-1_amd64.deb -y

# 3. バージョン固定（自動更新による互換性問題を防止）
sudo apt-mark hold java-17-amazon-corretto-jdk

# 4. インストールパスの確認（Tomcat設定時に必要）
# パス: /usr/lib/jvm/java-17-amazon-corretto-jdk
java -version
```

---

## 3. Database Setup (PostgreSQL 16)

セキュリティのため、SSHトンネリング経由のみで接続できるよう設定します。

### 3-1. インストール（16.11バージョン）

```bash
# 1. 公式リポジトリのキーと一覧を登録
sudo mkdir -p /usr/share/postgresql-common/pgdg
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo gpg --dearmor -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg > /dev/null
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.gpg] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt update

# 2. 特定バージョンのインストールと固定
sudo apt install postgresql-16=16.11-1.pgdg22.04+1 -y
sudo apt-mark hold postgresql-16
```

### 3-2. 接続許可設定

1. `sudo nano /etc/postgresql/16/main/postgresql.conf`
   - `listen_addresses = '*'` に変更

2. `sudo nano /etc/postgresql/16/main/pg_hba.conf`
   - `host all all 0.0.0.0/0 scram-sha-256` の行を追加

3. 再起動: `sudo systemctl restart postgresql`

---

## 4. Web Server Setup (Tomcat 10.1.49)

aptリポジトリにない最新バージョンを手動インストールし、サービスとして登録します。

### 4-1. ダウンロードとインストール

```bash
# 1. ダウンロードと解凍（/opt/tomcat）
cd /tmp
wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.49/bin/apache-tomcat-10.1.49.tar.gz
sudo mkdir -p /opt/tomcat
sudo tar -xzvf apache-tomcat-10.1.49.tar.gz -C /opt/tomcat --strip-components=1

# 2. 専用ユーザーの作成と権限付与
sudo groupadd tomcat
sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
sudo chown -R tomcat: /opt/tomcat
sudo chmod +x /opt/tomcat/bin/*.sh
```

### 4-2. Systemdサービスへの登録

`sudo nano /etc/systemd/system/tomcat.service` ファイルを作成し、以下の内容を入力します。

```ini
[Unit]
Description=Apache Tomcat 10.1.49 Web Application Container
After=network.target

[Service]
Type=forking

# ★ Javaのパス指定（Corretto 17）
Environment="JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto-jdk"

Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_BASE=/opt/tomcat"

# ★ 1GB RAM最適化（Swapを過信せず512MBに制限）
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

### 4-3. 起動と有効化

```bash
sudo systemctl daemon-reload
sudo systemctl start tomcat
sudo systemctl enable tomcat
```

---

## 5. Firewall Configuration (8080 Port)

Minimalバージョンではiptables-persistentでルールを永続化しないと、再起動後に接続できなくなります。

### 5-1. OCI Console（クラウドファイアウォール）

- **Ingress Rules:** プロトコルTCP、ポート8080、CIDR `0.0.0.0/0` を追加。

### 5-2. Ubuntuサーバー（内部ファイアウォール）

```bash
# 1. 8080ポート開放ルールを追加（既存ルールより上位に挿入）
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 8080 -j ACCEPT

# 2. ルールの確認
sudo iptables -L -n -v --line-numbers | grep 8080

# 3. ★ ルールの永続化（Minimalバージョンでは必須）
sudo netfilter-persistent save
```

---

## 6. Domain Setup (DuckDNS)

無料の動的DNSサービスであるDuckDNSを使って、OCIサーバーのパブリックIPにドメインを紐付けます。

### 6-1. DuckDNSドメインの登録

1. [https://www.duckdns.org](https://www.duckdns.org) にアクセスしてログイン
2. 希望のサブドメインを入力（例: `carrot-market`）→ **add domain** をクリック
3. **current ip** にOCIサーバーのパブリックIPを入力して **update ip** をクリック
4. トークンをコピー（Certbot認証時に必要）

### 6-2. IP自動更新スクリプト（任意）

OCI Free TierはIPが固定のため必須ではありませんが、動的IP環境では以下のスクリプトで自動更新します。

```bash
# スクリプトの作成
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

# crontabで5分ごとに実行
crontab -e
# 以下の行を追加
*/5 * * * * ~/duckdns/duck.sh >/dev/null 2>&1
```

---

## 7. Nginx Setup (Reverse Proxy)

外部の80/443トラフィックを前段で受け取り、Tomcat（localhost:8080）に転送するリバースプロキシを構成します。

### 7-1. Nginxのインストール

```bash
sudo apt update
sudo apt install nginx -y
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 7-2. ファイアウォールに80/443ポートを追加

**OCI Console:**
- Ingress RulesにTCP 80、TCP 443を追加

**Ubuntu内部ファイアウォール:**
```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### 7-3. Nginx設定ファイルの作成

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
# 設定の有効化
sudo ln -s /etc/nginx/sites-available/carrot-market /etc/nginx/sites-enabled/
sudo nginx -t        # 文法チェック
sudo systemctl reload nginx
```

---

## 8. HTTPS Setup (Certbot + Let's Encrypt)

CertbotでLet's Encryptの無料SSL証明書を取得し、Nginxに連携します。

### 8-1. Certbotのインストール

```bash
sudo apt install certbot python3-certbot-nginx -y
```

### 8-2. 証明書の取得

```bash
sudo certbot --nginx -d YOUR_DOMAIN.duckdns.org
```

メールアドレスの入力と利用規約への同意後、取得が完了するとCertbotがNginxの設定を自動で修正します。

### 8-3. 自動更新の確認

Let's Encryptの証明書は90日ごとに失効しますが、Certbotが自動更新タイマーを登録します。

```bash
# 自動更新タイマーの確認
sudo systemctl status certbot.timer

# 更新テスト（実際の取得なしにシミュレーション）
sudo certbot renew --dry-run
```

### 8-4. 最終Nginx設定（Certbot適用後）

Certbot適用後、`/etc/nginx/sites-available/carrot-market` は以下のように変更されます。

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN.duckdns.org;
    return 301 https://$host$request_uri;  # HTTP → HTTPSリダイレクト
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
