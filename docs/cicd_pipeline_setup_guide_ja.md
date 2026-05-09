# CI/CD Pipeline Setup Guide (GitHub Actions + OCI)

このガイドは、GitHub Actionsを使って`develop`ブランチへのpush時にOCIサーバーへ自動デプロイするパイプラインの構築手順を説明します。

---

## 0. Architecture Overview

```
[GitHub Repository]
       │
       │ develop branch push
       ▼
[GitHub Actions Runner]
       │
       ├── 1. ソースコードのチェックアウト
       ├── 2. JDK 17 のセットアップ
       ├── 3. db.properties の生成 (Secrets注入)
       ├── 4. Mavenビルド (WAR生成)
       ├── 5. SCPでWAR転送
       └── 6. SSHでデプロイスクリプト実行
               │
               ▼
[OCI Server (Ubuntu 22.04)]
       │
       ├── /opt/tomcat/scripts/deploy.sh
       │     ├── 既存WARのバックアップ
       │     ├── Tomcat停止
       │     ├── WAR差し替え
       │     └── Tomcat起動
       │
       └── /opt/tomcat/webapps/carrot-market-api.war
```

---

## 1. OCI Server Preparation

デプロイ自動化のため、サーバーにSSHキーとスクリプトを設定します。

### 1-1. SSHキーの生成（ローカルPCで実行）

GitHub Actionsがサーバーに接続する際に使用するSSHキーを生成します。

```bash
# ED25519キーの生成（推奨）
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy_key

# またはRSAキーの生成
ssh-keygen -t rsa -b 4096 -C "github-actions-deploy" -f ~/.ssh/github_deploy_key
```

### 1-2. 公開鍵をOCIサーバーに登録

```bash
# OCIサーバーにSSH接続後に実行
# ローカルで生成した公開鍵の内容をコピーして追加
echo "ssh-ed25519 AAAA... github-actions-deploy" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

### 1-3. デプロイディレクトリの作成

```bash
# OCIサーバーで実行
sudo mkdir -p /opt/tomcat/backup
sudo mkdir -p /opt/tomcat/scripts
sudo chown -R tomcat:tomcat /opt/tomcat/backup
sudo chown -R ubuntu:ubuntu /opt/tomcat/scripts
```

### 1-4. デプロイスクリプトの作成

`sudo nano /opt/tomcat/scripts/deploy.sh` ファイルを作成し、以下の内容を入力します。

```bash
#!/bin/bash
set -e

TOMCAT_HOME="/opt/tomcat"
WAR_NAME="carrot-market-api.war"
APP_NAME="carrot-market-api"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo "[$(date)] デプロイ開始"

# 1. 既存WARのバックアップ
if [ -f ${TOMCAT_HOME}/webapps/${WAR_NAME} ]; then
    sudo -u tomcat cp ${TOMCAT_HOME}/webapps/${WAR_NAME} \
        ${TOMCAT_HOME}/backup/${APP_NAME}_${TIMESTAMP}.war
    echo "[$(date)] バックアップ完了: ${APP_NAME}_${TIMESTAMP}.war"
fi

# 2. Tomcat停止
sudo systemctl stop tomcat || true
sleep 3

# 3. 既存デプロイの削除
sudo rm -rf ${TOMCAT_HOME}/webapps/${APP_NAME}
sudo rm -f ${TOMCAT_HOME}/webapps/${WAR_NAME}

# 4. 新しいWARのデプロイ
sudo mv /tmp/${WAR_NAME} ${TOMCAT_HOME}/webapps/
sudo chown tomcat:tomcat ${TOMCAT_HOME}/webapps/${WAR_NAME}

# 5. Tomcat起動
sudo systemctl start tomcat

# 6. 古いバックアップの削除（直近5件のみ保持）
cd ${TOMCAT_HOME}/backup
ls -t ${APP_NAME}_*.war 2>/dev/null | tail -n +6 | xargs -r sudo rm -f

echo "[$(date)] デプロイ完了"
```

### 1-5. ロールバックスクリプトの作成（任意）

`sudo nano /opt/tomcat/scripts/rollback.sh` ファイルを作成します。

```bash
#!/bin/bash
set -e

TOMCAT_HOME="/opt/tomcat"
WAR_NAME="carrot-market-api.war"
APP_NAME="carrot-market-api"

# 最新のバックアップファイルを探す
BACKUP_FILE=$(ls -t ${TOMCAT_HOME}/backup/${APP_NAME}_*.war 2>/dev/null | head -1)

if [ -z "$BACKUP_FILE" ]; then
    echo "バックアップファイルが見つかりません。"
    exit 1
fi

echo "[$(date)] ロールバック開始: $BACKUP_FILE"

sudo systemctl stop tomcat || true
sleep 3

sudo rm -rf ${TOMCAT_HOME}/webapps/${APP_NAME}
sudo rm -f ${TOMCAT_HOME}/webapps/${WAR_NAME}
sudo cp "$BACKUP_FILE" ${TOMCAT_HOME}/webapps/${WAR_NAME}
sudo chown tomcat:tomcat ${TOMCAT_HOME}/webapps/${WAR_NAME}

sudo systemctl start tomcat

echo "[$(date)] ロールバック完了"
```

### 1-6. スクリプトに実行権限を付与

```bash
sudo chmod +x /opt/tomcat/scripts/deploy.sh
sudo chmod +x /opt/tomcat/scripts/rollback.sh
```

### 1-7. sudo権限の設定（パスワードなしで実行）

`sudo visudo` コマンドでsudoersファイルを開き、以下の内容を追加します。

```bash
# GitHub Actionsデプロイ用sudo権限
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

GitHub リポジトリ → Settings → Secrets and variables → Actions → New repository secret

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `OCI_HOST` | OCIサーバーのパブリックIPアドレス | `123.456.789.012` |
| `OCI_USERNAME` | SSH接続ユーザー名 | `ubuntu` |
| `OCI_SSH_PRIVATE_KEY` | SSH秘密鍵の全内容 | `-----BEGIN OPENSSH PRIVATE KEY-----...` |
| `DB_URL` | PostgreSQL接続URL | `jdbc:postgresql://localhost:5432/carrot_market` |
| `DB_USERNAME` | DBユーザー名 | `postgres` |
| `DB_PASSWORD` | DBパスワード | `your_secure_password` |

### SSH秘密鍵の登録方法

```bash
# ローカルで秘密鍵の内容を確認
cat ~/.ssh/github_deploy_key

# 出力された全内容（-----BEGIN ~ -----END を含む）をコピーして
# OCI_SSH_PRIVATE_KEY Secretに登録
```

---

## 3. GitHub Actions Workflow

プロジェクトルートに `.github/workflows/deploy.yml` ファイルを作成します。

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
      # 1. ソースコードのチェックアウト
      - name: Checkout code
        uses: actions/checkout@v4

      # 2. JDK 17 のセットアップ
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      # 3. 本番環境用 db.properties の生成
      - name: Create db.properties
        run: |
          mkdir -p src/main/resources
          cat << EOF > src/main/resources/db.properties
          db.url=${{ secrets.DB_URL }}
          db.username=${{ secrets.DB_USERNAME }}
          db.password=${{ secrets.DB_PASSWORD }}
          db.driver=org.postgresql.Driver
          EOF

      # 4. Mavenビルド
      - name: Build with Maven
        run: mvn clean package -DskipTests

      # 5. WARファイルの確認
      - name: Verify WAR file
        run: |
          if [ ! -f target/${{ env.WAR_NAME }} ]; then
            echo "WAR file not found!"
            exit 1
          fi
          ls -lh target/${{ env.WAR_NAME }}

      # 6. SSHキーのセットアップ
      - name: Setup SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.OCI_SSH_PRIVATE_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ secrets.OCI_HOST }} >> ~/.ssh/known_hosts

      # 7. WARファイルの転送
      - name: Upload WAR to server
        run: |
          scp target/${{ env.WAR_NAME }} \
            ${{ secrets.OCI_USERNAME }}@${{ secrets.OCI_HOST }}:/tmp/${{ env.WAR_NAME }}

      # 8. デプロイスクリプトの実行
      - name: Deploy
        run: |
          ssh ${{ secrets.OCI_USERNAME }}@${{ secrets.OCI_HOST }} \
            'bash /opt/tomcat/scripts/deploy.sh'

      # 9. SSHキーの削除
      - name: Cleanup SSH key
        if: always()
        run: rm -rf ~/.ssh/id_rsa
```

---

## 4. Deployment Test

### 4-1. 手動テスト（ローカルからSSH接続確認）

```bash
# SSH接続テスト
ssh -i ~/.ssh/github_deploy_key ubuntu@<OCI_HOST>

# デプロイスクリプトの手動実行テスト
bash /opt/tomcat/scripts/deploy.sh
```

### 4-2. 自動デプロイのテスト

```bash
# developブランチにコミット後push
git checkout develop
git add .
git commit -m "test: CI/CDパイプラインテスト"
git push origin develop
```

### 4-3. デプロイ結果の確認

**GitHub Actionsの確認:**
- GitHubリポジトリ → Actionsタブ → ワークフローの実行状態を確認

**OCIサーバーでの確認:**
```bash
# Tomcatログの確認
tail -f /opt/tomcat/logs/catalina.out

# アプリケーション状態の確認
curl http://localhost:8080/carrot-market-api/api/products

# バックアップファイルの確認
ls -lht /opt/tomcat/backup/
```

---

## 5. Rollback Procedure

デプロイ後に問題が発生した場合はロールバックを実行します。

```bash
# OCIサーバーで実行
bash /opt/tomcat/scripts/rollback.sh

# 特定バージョンにロールバックする場合はバックアップ一覧を確認後、手動で復元
ls -lht /opt/tomcat/backup/
sudo systemctl stop tomcat
sudo cp /opt/tomcat/backup/carrot-market-api_20240115_143022.war /opt/tomcat/webapps/carrot-market-api.war
sudo chown tomcat:tomcat /opt/tomcat/webapps/carrot-market-api.war
sudo systemctl start tomcat
```

---

## 6. Troubleshooting

| 問題 | 原因 | 解決方法 |
|------|------|---------|
| SSH接続失敗 | キーの不一致または権限の問題 | `chmod 600 ~/.ssh/id_rsa`、known_hostsの確認 |
| Permission denied | sudo権限不足 | visudoで権限を追加 |
| WAR配置後404 | Context pathの不一致 | WARファイル名が`carrot-market-api.war`か確認 |
| DB接続失敗 | db.properties生成失敗 | GitHub Actionsログでファイル生成ステップを確認 |
| Tomcat起動失敗 | メモリ不足 | `free -h`でメモリ確認、swap設定を確認 |
| ビルド失敗 | MavenまたはJavaのバージョン問題 | `pom.xml`のJavaバージョンとActions設定が一致しているか確認 |

### ログ確認コマンド

```bash
# Tomcatログ
tail -100 /opt/tomcat/logs/catalina.out

# システムログ
journalctl -u tomcat -n 50

# GitHub Actionsログ
# GitHubリポジトリ → Actions → 該当ワークフローをクリック → 各stepのログを確認
```

---

## 7. Security Considerations

1. **SSHキーの管理**
   - 秘密鍵はGitHub Secretsにのみ保存
   - ローカルの秘密鍵はデプロイ専用としてのみ使用
   - 定期的なキーの更新を推奨

2. **DB認証情報**
   - `db.properties`は絶対にGitにコミットしない（`.gitignore`に含める）
   - 本番DBのパスワードは強力なものを使用

3. **サーバーアクセス制限**
   - OCIセキュリティリストでSSH（22番）ポートのアクセスIPを制限することを推奨
   - デプロイ用SSHキーはデプロイ作業にのみ使用

---

## 8. File Structure Summary

```
プロジェクト (GitHub)
├── .github/
│   └── workflows/
│       └── deploy.yml          # GitHub Actionsワークフロー
├── src/main/resources/
│   └── db.properties           # .gitignore (CI/CDで生成)
└── pom.xml

OCIサーバー
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
    └── authorized_keys         # GitHub Actions用公開鍵
```
