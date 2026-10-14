-- ============================================================
-- 增量迁移：买卖双方私聊模块（WebSocket 实时聊天）
-- ============================================================
-- 可重复执行，不删除已有数据
-- 执行：mysql -u root -p123456 esshop < migration_chat.sql
-- ============================================================

USE esshop;

-- ==================== 会话表 ====================
-- 一条记录代表"某买家与某卖家围绕某商品"的一个会话
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `buyer_id` BIGINT NOT NULL COMMENT '买家ID',
    `seller_id` BIGINT NOT NULL COMMENT '卖家ID',
    `product_id` BIGINT DEFAULT NULL COMMENT '关联商品ID（可为空，如从消息中心发起）',
    `last_message` VARCHAR(255) DEFAULT NULL COMMENT '最后一条消息预览',
    `last_time` DATETIME DEFAULT NULL COMMENT '最后消息时间（用于排序）',
    `buyer_unread` INT NOT NULL DEFAULT 0 COMMENT '买家未读数',
    `seller_unread` INT NOT NULL DEFAULT 0 COMMENT '卖家未读数',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 同一对买卖家围绕同一商品只保留一个会话
    UNIQUE KEY `uk_buyer_seller_product` (`buyer_id`, `seller_id`, `product_id`),
    KEY `idx_buyer_id` (`buyer_id`),
    KEY `idx_seller_id` (`seller_id`),
    KEY `idx_last_time` (`last_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- ==================== 私聊消息表 ====================
CREATE TABLE IF NOT EXISTS `private_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` VARCHAR(1000) NOT NULL COMMENT '消息内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私聊消息表';

SELECT '✅ conversation / private_message 表已就绪' AS 结果;
