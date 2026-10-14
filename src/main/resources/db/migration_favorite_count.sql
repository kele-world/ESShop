-- ============================================================
-- 增量迁移：为商品和帖子增加收藏计数
-- ============================================================
-- 可重复执行（用存储过程判断列是否存在）
--
-- 执行：mysql -u root -p123456 esshop < migration_favorite_count.sql
-- ============================================================

USE esshop;

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

-- 商品表：新增 favorite_count
CALL add_col_if_missing('product', 'favorite_count',
    '`favorite_count` INT NOT NULL DEFAULT 0 COMMENT ''收藏数'' AFTER `view_count`');

-- 帖子表：新增 favorite_count
CALL add_col_if_missing('community_post', 'favorite_count',
    '`favorite_count` INT NOT NULL DEFAULT 0 COMMENT ''收藏数'' AFTER `like_count`');

DROP PROCEDURE IF EXISTS `add_col_if_missing`;

-- ============================================================
-- 回填历史数据的收藏数
-- ============================================================
-- 把 user_favorite 里已有的收藏记录统计回各自的表，
-- 避免老数据收藏数一直是 0
UPDATE `product` p
SET p.`favorite_count` = (
    SELECT COUNT(*) FROM `user_favorite` f
    WHERE f.`type` = 'PRODUCT' AND f.`target_id` = p.`id`
);

UPDATE `community_post` c
SET c.`favorite_count` = (
    SELECT COUNT(*) FROM `user_favorite` f
    WHERE f.`type` = 'POST' AND f.`target_id` = c.`id`
);

SELECT
    (SELECT COUNT(*) FROM `product` WHERE favorite_count > 0) AS '有收藏的商品数',
    (SELECT COUNT(*) FROM `community_post` WHERE favorite_count > 0) AS '有收藏的帖子数';
