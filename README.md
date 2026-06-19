# demo-message-service

`demo-message-service` là một ứng dụng Spring Boot REST API đơn giản dùng để demo pipeline triển khai với Jenkins, Ansible và Out-of-Band Deployment Monitor (OBDM).

Ứng dụng không dùng database, không có authentication, không có frontend. API trả về thông tin cấu hình đọc từ `application.yml`, giúp dễ kiểm chứng khi có thay đổi artifact hoặc cấu hình trên server.

## Tech stack

- Java 17
- Spring Boot 3.x
- Maven
- Executable JAR
- REST API only

## Cấu trúc project

```text
demo-message-service/
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java/com/example/demomessage
    │   │   ├── DemoMessageServiceApplication.java
    │   │   ├── config/DemoProperties.java
    │   │   └── controller/DemoController.java
    │   └── resources/application.yml
    └── test
        └── java/com/example/demomessage
            ├── config/DemoPropertiesBindingTest.java
            └── controller/DemoControllerTest.java
```

## Cấu hình mặc định

File `src/main/resources/application.yml`:

```yaml
server:
  port: 8080

demo:
  serviceName: "demo-message-service"
  version: "1.0.0"
  environment: "production"
  message: "Hello from approved deployment"
```

Ứng dụng đọc các giá trị `demo.*` bằng `@ConfigurationProperties` trong class `DemoProperties`.

## API

### GET /health

Response mẫu:

```json
{
  "status": "ok",
  "service": "demo-message-service"
}
```

### GET /version

Response mẫu:

```json
{
  "version": "1.0.0"
}
```

### GET /message

Response mẫu:

```json
{
  "service": "demo-message-service",
  "version": "1.0.0",
  "environment": "production",
  "message": "Hello from approved deployment"
}
```

### GET /config

Trả toàn bộ thông tin demo config hiện tại:

```json
{
  "service": "demo-message-service",
  "version": "1.0.0",
  "environment": "production",
  "message": "Hello from approved deployment"
}
```

## 1. Build bằng Maven

```bash
mvn clean package
```

Sau khi build thành công, artifact được tạo tại:

```text
target/demo-message-service.jar
```

## 2. Chạy local bằng config đóng gói trong JAR

```bash
java -jar target/demo-message-service.jar
```

Kiểm tra API:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/version
curl http://localhost:8080/message
curl http://localhost:8080/config
```

## 3. Chạy với external application.yml

Tạo file `application.yml` cùng thư mục với JAR:

```yaml
server:
  port: 8080

demo:
  serviceName: "demo-message-service"
  version: "1.0.1"
  environment: "production"
  message: "Hello from external config"
```

Chạy ứng dụng với external config:

```bash
java -jar demo-message-service.jar --spring.config.location=file:./application.yml
```

Hoặc nếu chạy từ thư mục project sau khi build:

```bash
java -jar target/demo-message-service.jar --spring.config.location=file:./application.yml
```

Kiểm tra message:

```bash
curl http://localhost:8080/message
```

Nếu sửa `application.yml` và restart app, response `/message` sẽ thay đổi theo nội dung config mới.

## 4. Bối cảnh demo Jenkins + Ansible + OBDM

### Jenkins

Jenkins build ứng dụng bằng lệnh:

```bash
mvn clean package
```

Artifact đầu ra:

```text
target/demo-message-service.jar
```

### Ansible

Ansible copy các file triển khai vào thư mục runtime:

```text
/opt/app/demo-message/current/demo-message-service.jar
/opt/app/demo-message/current/application.yml
/opt/app/demo-message/current/VERSION
```

Ví dụ sau khi copy, service có thể được chạy từ thư mục:

```bash
cd /opt/app/demo-message/current
java -jar demo-message-service.jar --spring.config.location=file:./application.yml
```

### Out-of-Band Deployment Monitor

Ứng dụng này phù hợp để demo OBDM vì có hai loại thay đổi dễ quan sát:

1. Shadow deployment config change

   Nếu ai đó sửa thủ công file:

   ```text
   /opt/app/demo-message/current/application.yml
   ```

   Sau khi restart app, response `/message` hoặc `/config` sẽ thay đổi. OBDM có thể phát hiện file config bị `MODIFY`, ví dụ severity `MEDIUM`.

2. Shadow deployment artifact change

   Nếu ai đó thay thủ công file:

   ```text
   /opt/app/demo-message/current/demo-message-service.jar
   ```

   OBDM có thể phát hiện artifact bị `MODIFY`, ví dụ severity `HIGH`.

Mục tiêu của demo là cho thấy pipeline chuẩn phải đi qua Jenkins và Ansible, còn thay đổi ngoài luồng trên server sẽ bị OBDM phát hiện.

## Gợi ý systemd service cho môi trường Linux

Ví dụ service file:

```ini
[Unit]
Description=Demo Message Service
After=network.target

[Service]
User=app
WorkingDirectory=/opt/app/demo-message/current
ExecStart=/usr/bin/java -jar /opt/app/demo-message/current/demo-message-service.jar --spring.config.location=file:/opt/app/demo-message/current/application.yml
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

## 5. CI/CD bằng Jenkins + Ansible

Project đã có sẵn pipeline và playbook deploy mẫu:

```text
Jenkinsfile
ansible/deploy.yml
ansible/inventories/production.ini
ansible/group_vars/demo_message_servers.yml
ansible/templates/demo-message-service.service.j2
docs/CI_CD.md
scripts/smoke-test.sh
```

Tóm tắt luồng CI/CD:

```text
Jenkins checkout -> mvn clean test -> mvn package -> archive artifact -> Ansible deploy -> systemd restart -> health check
```

Jenkins build artifact:

```text
target/demo-message-service.jar
```

Ansible deploy các file approved vào:

```text
/opt/app/demo-message/current/demo-message-service.jar
/opt/app/demo-message/current/application.yml
/opt/app/demo-message/current/VERSION
```

Xem hướng dẫn chi tiết tại:

```text
docs/CI_CD.md
```
