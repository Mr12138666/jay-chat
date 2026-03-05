# 1Panel 部署指南

## 前置准备

1. 确保云服务器已安装 1Panel
2. 确保 MySQL 数据库已创建（数据库名：`jay_chat`）
3. 确保服务器已安装 Docker 和 Docker Compose（1Panel 通常自带）

## 步骤 1：初始化数据库

### 方法一：通过 1Panel 数据库管理界面

1. 登录 1Panel
2. 进入 **数据库** → **MySQL** → 选择你的数据库实例
3. 点击 **SQL 编辑器** 或 **phpMyAdmin**
4. 选择数据库 `jay_chat`
5. 执行以下 SQL 脚本（复制 `database/init_tables.sql` 的内容）：

```sql
-- 创建聊天室相关表结构

-- 用户表（如果还没有）
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type` VARCHAR(16) NOT NULL COMMENT 'single / group',
  `name` VARCHAR(128) DEFAULT NULL COMMENT '会话名（群聊用）',
  `owner_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建者',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话成员表
CREATE TABLE IF NOT EXISTS `chat_session_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_user` (`session_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL,
  `sender_id` BIGINT UNSIGNED NOT NULL,
  `content` TEXT NOT NULL,
  `content_type` VARCHAR(32) NOT NULL DEFAULT 'text',
  `sent_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_time` (`session_id`,`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 方法二：通过命令行（SSH）

```bash
# 连接到 MySQL
mysql -h 120.53.242.78 -u root -p123123 jay_chat

# 或者如果 MySQL 在本地
mysql -u root -p jay_chat

# 然后执行 SQL 文件
source /path/to/database/init_tables.sql
# 或者直接粘贴 SQL 内容
```

## 步骤 2：配置环境变量

1. 在项目根目录创建 `.env` 文件（参考 `.env.example`）
2. 修改以下配置：

```env
# MySQL 密码（如果与 application.properties 不同）
MYSQL_PASSWORD=123123

# JWT 密钥（建议使用复杂随机串，生产环境必须修改！）
JWT_SECRET=YourVerySecureRandomSecretKeyHere

# API 基础地址（改为你的服务器 IP 或域名）
API_BASE_URL=http://your-server-ip:8080
```

## 步骤 3：修改后端配置

编辑 `src/main/resources/application.properties`，确保数据库连接信息正确：

```properties
spring.datasource.url=jdbc:mysql://your-mysql-host:3306/jay_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=your_password
```

**注意**：如果使用 Docker Compose，MySQL 服务名是 `mysql`，URL 应该是：
```properties
spring.datasource.url=jdbc:mysql://mysql:3306/jay_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
```

## 步骤 4：修改前端配置

编辑 `client/.env.production`，设置生产环境的后端地址：

```env
VITE_API_BASE_URL=http://your-server-ip:8080
VITE_WS_BASE_URL=http://your-server-ip:8080
```

如果使用域名：
```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_WS_BASE_URL=wss://api.yourdomain.com
```

## 步骤 5：部署方式选择

### 方式 A：使用 Docker Compose（推荐）

1. 将整个项目上传到服务器
2. 在项目根目录执行：

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 方式 B：在 1Panel 中分别部署

#### 后端部署：

1. 在 1Panel 中创建 **应用** → **Java 项目**
2. 上传编译好的 JAR 文件（`target/jay-chat-0.0.1-SNAPSHOT.jar`）
3. 配置运行参数和端口（8080）
4. 配置环境变量（数据库连接、JWT 密钥等）

#### 前端部署：

1. 在项目根目录执行：
```bash
cd client
npm install
npm run build
```

2. 将 `client/dist` 目录上传到服务器
3. 在 1Panel 中创建 **网站** → **静态网站**
4. 配置 Nginx 反向代理（参考下面的 Nginx 配置）

## 步骤 6：Nginx 配置（如果单独部署前端）

在 1Panel 的网站配置中添加以下 Nginx 配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;  # 或你的服务器 IP
    
    root /path/to/client/dist;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 代理
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 86400;
    }
}
```

## 步骤 7：防火墙配置

确保以下端口已开放：
- **80**：前端访问（HTTP）
- **8080**：后端 API（如果直接访问）
- **3306**：MySQL（如果从外部连接，建议仅内网访问）

在 1Panel 中：**安全** → **防火墙** → 添加规则

## 步骤 8：验证部署

1. 访问前端地址：`http://your-server-ip` 或 `http://your-domain.com`
2. 测试注册和登录功能
3. 测试聊天功能
4. 检查后端日志确认无错误

## 常见问题

### 1. 数据库连接失败
- 检查 MySQL 是否运行
- 检查数据库用户名密码是否正确
- 检查防火墙是否允许连接
- 如果使用 Docker，确保网络配置正确

### 2. WebSocket 连接失败
- 检查 Nginx 配置中的 WebSocket 代理
- 检查防火墙是否开放相关端口
- 如果使用 HTTPS，需要配置 WSS（WebSocket Secure）

### 3. CORS 跨域错误
- 检查 `SecurityConfig.java` 中的 CORS 配置
- 确保允许的前端域名正确

### 4. JWT Token 无效
- 检查 JWT 密钥配置是否一致
- 检查 Token 是否过期

## 生产环境安全建议

1. **修改 JWT 密钥**：使用强随机字符串
2. **使用 HTTPS**：配置 SSL 证书（1Panel 支持 Let's Encrypt）
3. **数据库安全**：使用强密码，限制远程访问
4. **防火墙**：只开放必要端口
5. **定期备份**：配置数据库自动备份
