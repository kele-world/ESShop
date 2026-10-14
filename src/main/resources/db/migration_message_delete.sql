-- ============================================================
-- 增量迁移：聊天消息「单方删除」
-- ============================================================
-- 背景：原有的 deleted 字段是全局逻辑删除标记，
--       一旦置 1 双方都看不到，属于「真删除」。
--
-- 需求：删除只对自己生效，对方仍能看到，记录也不从数据库清除。
--       因此为每个参与者各设一个删除标记：
--         sender_deleted   —— 发送方删除了这条消息
--         receiver_deleted —— 接收方删除了这条消息
--       查询时按「当前用户是发送方还是接收方」过滤。
--
-- 可重复执行，不删除已有数据
-- 执行：mysql -u root -p123456 esshop < migration_message_delete.sql
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

-- 发送方删除标记
CALL add_col_if_missing('private_message', 'sender_deleted',
    '`sender_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''发送方是否删除'' AFTER `is_read`');

-- 接收方删除标记
CALL add_col_if_missing('private_message', 'receiver_deleted',
    '`receiver_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''接收方是否删除'' AFTER `sender_deleted`');

-- 加索引：查询时要按这两个标记过滤
DROP PROCEDURE IF EXISTS `add_col_if_missing`;

SELECT '✅ 聊天消息单方删除 迁移完成' AS 结果;
