USE jay_chat;

-- 若 chat_message 表不存在，则先创建（兼容未初始化数据库的情况）
CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  sender_id BIGINT UNSIGNED NOT NULL,
  bot_id BIGINT UNSIGNED NULL COMMENT 'AI机器人ID',
  content TEXT NOT NULL,
  content_type VARCHAR(32) NOT NULL DEFAULT 'text',
  reply_to_id BIGINT UNSIGNED DEFAULT NULL COMMENT '引用的消息ID',
  is_recalled TINYINT NOT NULL DEFAULT 0,
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_session_time (session_id, sent_at),
  KEY idx_bot_id (bot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 表已存在时，按需补齐 bot_id 字段（幂等）
SET @has_bot_id := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND COLUMN_NAME = 'bot_id'
);

SET @sql_add_bot_id := IF(
  @has_bot_id = 0,
  'ALTER TABLE chat_message ADD COLUMN bot_id BIGINT UNSIGNED NULL COMMENT ''AI机器人ID'' AFTER sender_id',
  'SELECT ''skip add column bot_id'' AS msg'
);
PREPARE stmt_add_bot_id FROM @sql_add_bot_id;
EXECUTE stmt_add_bot_id;
DEALLOCATE PREPARE stmt_add_bot_id;

-- 按需补齐 bot_id 索引（幂等）
SET @has_bot_idx := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_message'
    AND INDEX_NAME = 'idx_bot_id'
);

SET @sql_add_bot_idx := IF(
  @has_bot_idx = 0,
  'ALTER TABLE chat_message ADD KEY idx_bot_id (bot_id)',
  'SELECT ''skip add index idx_bot_id'' AS msg'
);
PREPARE stmt_add_bot_idx FROM @sql_add_bot_idx;
EXECUTE stmt_add_bot_idx;
DEALLOCATE PREPARE stmt_add_bot_idx;

-- 验证结果
SHOW COLUMNS FROM chat_message;
SHOW INDEX FROM chat_message;
