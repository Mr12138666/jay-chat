# JayChat

一个基于 Spring Boot + Vue 3 的实时聊天应用，支持私聊、群聊、AI 机器人等功能。

## 功能特性

- **用户系统**: 注册、登录、JWT 认证
- **私聊**: 点对点即时通讯
- **群聊**: 创建群组、邀请成员、群管理
- **AI 聊天**: 集成 DeepSeek AI 大模型，支持智能对话
- **消息**: 文字、图片、表情、消息引用、撤回
- **文件上传**: 阿里云 OSS 存储
- **实时通讯**: WebSocket 长连接

## 技术栈

### 后端
- Spring Boot 3.x
- Spring Security
- MyBatis + MySQL
- WebSocket
- Spring AI (DeepSeek)
- 阿里云 OSS

### 前端
- Vue 3 + Vite
- Vue Router
- Pinia (状态管理)
- Axios
- Markdown-it

## 快速开始

### 前置要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Node.js 18+
- 阿里云 OSS 账号（可选）
- DeepSeek API Key（可选）

### 配置

1. 克隆项目后，复制配置文件：

```bash
cp src/main/resources/application-local.yml src/main/resources/application-local.yml
```

2. 编辑 `application-local.yml`，配置你的数据库、OSS、AI 等信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jay_chat
    username: your_username
    password: your_password
  ai:
    deepseek:
      api-key: your-deepseek-api-key
```

3. 创建数据库：

```sql
CREATE DATABASE jay_chat DEFAULT CHARACTER SET utf8mb4;
```

### 启动后端

```bash
mvn spring-boot:run
```

后端默认端口：`8080`

### 启动前端

```bash
cd client
npm install
npm run dev
```

前端默认端口：`5173`

## 项目结构

```
jay-chat/
├── src/
│   └── main/
│       ├── java/com/sunrisejay/jaychat/
│       │   ├── ai/          # AI 相关
│       │   ├── config/      # 配置类
│       │   ├── controller/  # 控制器
│       │   ├── service/    # 业务逻辑
│       │   ├── mapper/      # 数据访问
│       │   ├── entity/     # 实体类
│       │   └── dto/        # 数据传输对象
│       └── resources/
│           ├── mappers/     # MyBatis XML
│           └── application*.yml
├── client/                  # Vue 前端
│   ├── src/
│   │   ├── components/     # 组件
│   │   ├── views/          # 页面
│   │   ├── composables/    # 组合式 API
│   │   └── api/            # API 接口
│   └── package.json
└── pom.xml
```

## 配置说明

| 配置文件 | 用途 |
|---------|------|
| `application.yml` | 公共配置 |
| `application-local.yml` | 本地开发配置（推送到 GitHub） |
| `application-prod.yml` | 生产环境配置（不推送） |

启动时通过 `--spring.profiles.active=prod` 指定环境。

## 接口文档

启动后访问：`http://localhost:8080/swagger-ui.html`

## 许可证

Apache License 2.0
