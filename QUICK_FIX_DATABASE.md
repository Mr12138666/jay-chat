# 快速修复：数据库表不存在

## 问题
错误信息：`Table 'jay_chat.chat_session' doesn't exist`

## 解决方案

### 方法一：通过 1Panel 执行 SQL（推荐）

1. 登录 1Panel
2. 进入 **数据库** → **MySQL**
3. 找到你的数据库实例，点击 **管理** 或 **SQL 编辑器**
4. 选择数据库 `jay_chat`
5. 复制并执行以下 SQL：

```sql
-- 用户表
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

### 方法二：通过命令行执行

```bash
# 连接到 MySQL（根据你的实际情况修改连接信息）
mysql -h 120.53.242.78 -u root -p123123 jay_chat < database/init_tables.sql

# 或者交互式执行
mysql -h 120.53.242.78 -u root -p123123 jay_chat
# 然后粘贴上面的 SQL 语句
```

### 方法三：使用 MySQL 客户端工具

如果你使用 Navicat、DBeaver、phpMyAdmin 等工具：
1. 连接到数据库 `jay_chat`
2. 打开 SQL 编辑器
3. 执行 `database/init_tables.sql` 文件中的内容

## 验证

执行完成后，检查表是否创建成功：

```sql
SHOW TABLES;
```

应该看到以下 4 个表：
- `user`
- `chat_session`
- `chat_session_member`
- `chat_message`

## 完成后

重启后端服务，错误应该消失。
