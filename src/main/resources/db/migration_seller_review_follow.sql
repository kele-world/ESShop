-- ============================================================
-- 增量迁移：卖家评价改造 + 关注功能
-- ============================================================
-- 背景：二手商品一件只交易一次，评价挂在商品上没有意义。
--       改为评价「卖家」，建立用户信用体系，解决交易信任问题。
--
-- 可重复执行，不删除已有数据
-- 执行：mysql -u root -p123456 esshop < migration_seller_review_follow.sql
-- ============================================================

USE esshop;

-- ==================== 1. 为评价表增加「被评价人」字段 ====================
DROP PROCEDURE IF EXISTS `add_col_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_col_if_missing`(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT·
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

-- 被评价的卖家
CALL add_col_if_missing('product_review', 'seller_id',
    '`seller_id` BIGINT DEFAULT NULL COMMENT ''被评价的卖家ID'' AFTER `user_id`');

DROP PROCEDURE IF EXISTS `add_col_if_missing`;

-- 回填历史数据：评价的卖家 = 对应订单的卖家
UPDATE `product_review` r
SET r.`seller_id` = (SELECT o.`seller_id` FROM `order_info` o WHERE o.`id` = r.`order_id`)
WHERE r.`seller_id` IS NULL;

-- 加上索引，卖家主页要按 seller_id 查评价
CREATE INDEX `idx_seller_id` ON `product_review` (`seller_id`);

-- ==================== 2. 用户关注表 ====================
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `follower_id` BIGINT NOT NULL COMMENT '关注者（粉丝）ID',
    `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    -- 同一对关系只能有一条
    UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    KEY `idx_follower` (`follower_id`),
    KEY `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ==================== 3. 用户表增加统计字段（可选，用于快速展示） ====================
-- 不建冗余字段，粉丝数/关注数/商品数实时统计即可（数据量小，性能无压力）

SELECT '✅ 卖家评价 + 关注功能 迁移完成' AS 结果;
