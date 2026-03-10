-- AI 机器人功能数据库脚本
-- 创建时间: 2026-03-09
use jay_chat;
-- AI 机器人表
CREATE TABLE IF NOT EXISTS `ai_bot` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
  `name` VARCHAR(64) NOT NULL COMMENT '机器人名称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '机器人头像',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '机器人描述',
  `system_prompt` TEXT COMMENT '系统提示词',
  `model` VARCHAR(32) DEFAULT 'deepseek-chat' COMMENT '使用的模型',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 群聊中的机器人关联表
CREATE TABLE IF NOT EXISTS `chat_session_bot` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  `bot_id` BIGINT UNSIGNED NOT NULL COMMENT '机器人ID',
  `added_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_bot` (`session_id`, `bot_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_bot_id` (`bot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
