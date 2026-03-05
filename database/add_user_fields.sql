-- 添加用户表的 last_login_at 和 last_message_at 字段
-- 请在数据库中执行此SQL脚本

USE jay_chat;

-- 添加上次登录时间字段
ALTER TABLE `user` 
ADD COLUMN `last_login_at` DATETIME DEFAULT NULL COMMENT '上次登录时间' AFTER `avatar`;

-- 添加上次发言时间字段
ALTER TABLE `user` 
ADD COLUMN `last_message_at` DATETIME DEFAULT NULL COMMENT '上次发言时间' AFTER `last_login_at`;
