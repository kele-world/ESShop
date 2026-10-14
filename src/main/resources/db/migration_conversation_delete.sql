-- ============================================================
-- 增量迁移：会话「单方删除」
-- ============================================================
-- 场景：左边会话列表里有不想看到的对话，可以直接移除。
--       移除只对自己生效，对方列表里照常保留，
--       并且之后对方再发消息时，会话会重新出现在我的列表里。
--
-- 与消息删除同理：不用全局 @TableLogic（那会导致双方都看不到），
-- 而是买卖双方各设一个删除标记。
--
-- 可重复执行，不删除已有数据
-- 执行：mysql -u root -p123456 esshop < migration_conversation_delete.sql
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

-- 买家是否从列表中移除了这个会话
CALL add_col_if_missing('conversation', 'buyer_deleted',
    '`buyer_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''买家是否移除会话'' AFTER `seller_unread`');

-- 卖家是否从列表中移除了这个会话
CALL add_col_if_missing('conversation', 'seller_deleted',
    '`seller_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''卖家是否移除会话'' AFTER `buyer_deleted`');

DROP PROCEDURE IF EXISTS `add_col_if_missing`;

SELECT '✅ 会话单方删除 迁移完成' AS 结果;
