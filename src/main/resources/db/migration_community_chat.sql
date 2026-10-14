-- ============================================================
-- 增量迁移脚本：社区模块 + 智能客服模块
-- ============================================================
-- 适用场景：数据库里已有 user/category/product/order_info 等核心表，
--           但缺少后期新增的社区和客服相关表。
--
-- 特点：全部使用 CREATE TABLE IF NOT EXISTS + INSERT IGNORE
--       —— 可以重复执行，不会删除或覆盖任何已有数据
--
-- 执行方式（命令行）：
--   mysql -u root -p esshop < migration_community_chat.sql
-- 或直接在 Navicat / DataGrip 里打开本文件执行
-- ============================================================

USE esshop;

-- ==================== 1. 社区帖子表 ====================
CREATE TABLE IF NOT EXISTS `community_post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(100) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `images` VARCHAR(1000) DEFAULT NULL COMMENT '图片，多个用逗号分隔',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/OFF_SHELF',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

-- 若表已存在但缺列，补上（MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，用存储过程判断）
DROP PROCEDURE IF EXISTS `add_col_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_col_if_missing`(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'esshop' AND TABLE_NAME = tbl AND COLUMN_NAME = col
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_col_if_missing('community_post', 'category_id',   '`category_id` BIGINT DEFAULT NULL COMMENT ''分类ID'' AFTER `user_id`');
CALL add_col_if_missing('community_post', 'comment_count', '`comment_count` INT NOT NULL DEFAULT 0 COMMENT ''评论数'' AFTER `like_count`');

DROP PROCEDURE IF EXISTS `add_col_if_missing`;

-- ==================== 2. 社区分类表 ====================
CREATE TABLE IF NOT EXISTS `community_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区分类表';

-- 预置分类（用 NOT EXISTS 避免重复插入）
INSERT INTO `community_category` (`name`, `sort`)
SELECT * FROM (SELECT '闲置好物' AS n, 1 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `community_category` WHERE `name` = '闲置好物');
INSERT INTO `community_category` (`name`, `sort`)
SELECT * FROM (SELECT '校园生活' AS n, 2 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `community_category` WHERE `name` = '校园生活');
INSERT INTO `community_category` (`name`, `sort`)
SELECT * FROM (SELECT '二手心得' AS n, 3 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `community_category` WHERE `name` = '二手心得');
INSERT INTO `community_category` (`name`, `sort`)
SELECT * FROM (SELECT '求助问答' AS n, 4 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `community_category` WHERE `name` = '求助问答');
INSERT INTO `community_category` (`name`, `sort`)
SELECT * FROM (SELECT '其他' AS n, 99 AS s) t
WHERE NOT EXISTS (SELECT 1 FROM `community_category` WHERE `name` = '其他');

-- ==================== 3. 帖子点赞表 ====================
CREATE TABLE IF NOT EXISTS `post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- ==================== 4. 用户收藏表 ====================
CREATE TABLE IF NOT EXISTS `user_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
    `type` VARCHAR(10) NOT NULL COMMENT 'POST-帖子, PRODUCT-商品',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target_type` (`user_id`, `target_id`, `type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ==================== 5. 帖子评论表 ====================
CREATE TABLE IF NOT EXISTS `post_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子评论表';

-- ==================== 6. 智能客服知识库表 ====================
CREATE TABLE IF NOT EXISTS `chat_knowledge` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识ID',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `question` VARCHAR(200) NOT NULL COMMENT '标准问题',
    `keywords` VARCHAR(500) NOT NULL COMMENT '关键词，多个用逗号分隔',
    `answer` TEXT NOT NULL COMMENT '答案',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用, 1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能客服知识库表';

-- ==================== 7. 客服对话记录表 ====================
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID(未登录为空)',
    `role` VARCHAR(20) NOT NULL COMMENT 'USER-用户, ASSISTANT-客服',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `matched_id` BIGINT DEFAULT NULL COMMENT '命中的知识库ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服对话记录表';
