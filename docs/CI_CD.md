# Jenkins + Ansible CI/CD cho demo-message-service

Tài liệu này mô tả pipeline CI/CD mẫu cho `demo-message-service`.

## Luồng triển khai

```text
Developer push code
        |
        v
Jenkins checkout
        |
        v
mvn clean test
        |
        v
mvn -DskipTests package
        |
        v
Archive artifact + fingerprint
        |
        v
Ansible copy files to target server
        |
        v
systemd restart service
        |
        v
Health check GET /health
```

Artifact và config approved được deploy vào:

```text
/opt/app/demo-message/current/demo-message-service.jar
/opt/app/demo-message/current/application.yml
/opt/app/demo-message/current/VERSION
```

## File CI/CD được bổ sung

```text
Jenkinsfile
ansible/
├── ansible.cfg
├── deploy.yml
├── group_vars/demo_message_servers.yml
├── inventories/production.ini
└── templates/demo-message-service.service.j2
scripts/smoke-test.sh
```

## Yêu cầu trên Jenkins agent

Jenkins agent cần có:

- Java 17
- Maven 3.x
- Ansible
- SSH access tới target server
- Jenkins plugin `SSH Agent` nếu dùng block `sshagent`

Kiểm tra nhanh trên Jenkins agent:

```bash
java -version
mvn -version
ansible --version
```

## Jenkins credential

Tạo credential SSH private key trong Jenkins:

```text
Kind: SSH Username with private key
ID: ansible-ssh-key
Username: ubuntu hoặc user SSH phù hợp
Private Key: private key có quyền SSH vào server
```

Nếu đặt credential ID khác, sửa parameter `SSH_CREDENTIALS_ID` khi chạy pipeline.

## Cấu hình inventory Ansible

Sửa file:

```text
ansible/inventories/production.ini
```

Ví dụ:

```ini
[demo_message_servers]
demo-prod-01 ansible_host=192.0.2.10 ansible_user=ubuntu ansible_become=true

[demo_message_servers:vars]
ansible_python_interpreter=/usr/bin/python3
```

User SSH cần có quyền `sudo` để:

- tạo user/group `app`
- tạo thư mục `/opt/app/demo-message/current`
- cài systemd unit `/etc/systemd/system/demo-message-service.service`
- restart service

## Chạy Jenkins pipeline

Pipeline mặc định chạy các stage:

1. Checkout
2. Validate Toolchain
3. Test
4. Package
5. Archive Build Artifacts
6. Ansible Syntax Check
7. Deploy with Ansible

Các parameter quan trọng:

```text
RUN_DEPLOY=true
ANSIBLE_INVENTORY=ansible/inventories/production.ini
ANSIBLE_LIMIT=demo_message_servers
SSH_CREDENTIALS_ID=ansible-ssh-key
```

## Chạy Ansible thủ công từ máy control

Build app trước:

```bash
mvn clean package
mkdir -p target/deploy
cp src/main/resources/application.yml target/deploy/application.yml
cat > target/deploy/VERSION <<EOF_VERSION
app=demo-message-service
version=1.0.0
build_number=manual
git_commit=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
built_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF_VERSION
```

Chạy deploy:

```bash
ansible-playbook \
  -i ansible/inventories/production.ini \
  ansible/deploy.yml \
  --limit demo_message_servers \
  --extra-vars "artifact_src=$PWD/target/demo-message-service.jar" \
  --extra-vars "app_config_src=$PWD/target/deploy/application.yml" \
  --extra-vars "version_src=$PWD/target/deploy/VERSION"
```

## Kiểm tra sau deploy

Trên Jenkins hoặc máy cá nhân:

```bash
curl http://<server-ip>:8080/health
curl http://<server-ip>:8080/message
curl http://<server-ip>:8080/config
```

Hoặc dùng script:

```bash
./scripts/smoke-test.sh http://<server-ip>:8080
```

Trên server:

```bash
sudo systemctl status demo-message-service
sudo journalctl -u demo-message-service -f
ls -l /opt/app/demo-message/current/
cat /opt/app/demo-message/current/VERSION
```

## Demo OBDM

Pipeline này tạo baseline triển khai hợp lệ qua Jenkins + Ansible:

```text
/opt/app/demo-message/current/demo-message-service.jar
/opt/app/demo-message/current/application.yml
/opt/app/demo-message/current/VERSION
```

Kịch bản demo shadow config change:

```bash
sudo vi /opt/app/demo-message/current/application.yml
sudo systemctl restart demo-message-service
curl http://localhost:8080/message
```

Kết quả mong muốn:

```text
OBDM phát hiện MODIFY trên application.yml
Severity gợi ý: MEDIUM
```

Kịch bản demo shadow artifact change:

```bash
sudo cp /tmp/other-demo-message-service.jar /opt/app/demo-message/current/demo-message-service.jar
sudo systemctl restart demo-message-service
```

Kết quả mong muốn:

```text
OBDM phát hiện MODIFY trên demo-message-service.jar
Severity gợi ý: HIGH
```

## Rollback thủ công đơn giản

Playbook tạo backup trước mỗi lần deploy tại:

```text
/opt/app/demo-message/backups/<timestamp>/
```

Rollback thủ công:

```bash
sudo cp /opt/app/demo-message/backups/<timestamp>/demo-message-service.jar /opt/app/demo-message/current/demo-message-service.jar
sudo cp /opt/app/demo-message/backups/<timestamp>/application.yml /opt/app/demo-message/current/application.yml
sudo systemctl restart demo-message-service
```

Trong demo production thật, nên thay rollback thủ công bằng Jenkins job riêng có approval.
