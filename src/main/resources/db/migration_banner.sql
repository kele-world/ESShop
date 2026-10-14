-- ============================================================
-- 增量迁移：首页运营位（轮播图）
-- ============================================================
-- 用途：首页顶部轮播，展示平台运营内容
--       如「开学季活动」「平台公告」「新手指引」等
-- 特点：支持定时上下线（活动到期自动下线）、排序、启用开关
--
-- 执行：mysql -u root -p123456 esshop < migration_banner.sql
-- ============================================================

USE esshop;

CREATE TABLE IF NOT EXISTS `banner` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '运营位ID',
    `title` VARCHAR(100) NOT NULL COMMENT '主标题',
    `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题/说明文字',
    `image` VARCHAR(255) DEFAULT NULL COMMENT '背景图URL，留空则用渐变背景',
    `link` VARCHAR(255) DEFAULT NULL COMMENT '点击跳转地址，支持站内路径或外部链接',
    `button_text` VARCHAR(20) DEFAULT NULL COMMENT '按钮文字，留空则不显示按钮',
    `type` VARCHAR(20) NOT NULL DEFAULT 'ANNOUNCEMENT' COMMENT '类型: ACTIVITY-活动, ANNOUNCEMENT-公告, GUIDE-指引',
    `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，数字越小越靠前',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-停用, 1-启用',
    `start_time` DATETIME DEFAULT NULL COMMENT '生效开始时间，为空则立即生效',
    `end_time` DATETIME DEFAULT NULL COMMENT '生效结束时间，为空则永久有效',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled_sort` (`enabled`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页运营位表';

-- ==================== 预置运营位 ====================
-- 用 NOT EXISTS 避免重复插入

INSERT INTO `banner` (`title`, `subtitle`, `image`, `link`, `button_text`, `type`, `sort`)
SELECT '开学季 · 教材专场', '毕业学长学姐的教材低价转让，最高省 80%', NULL,
       '/product/list?categoryId=2', '去看看', 'ACTIVITY', 1
WHERE NOT EXISTS (SELECT 1 FROM `banner` WHERE `title` = '开学季 · 教材专场');

INSERT INTO `banner` (`title`, `subtitle`, `image`, `link`, `button_text`, `type`, `sort`)
SELECT '交易安全提醒', '请在校内公共场所当面交易，验货后再付款', NULL,
       NULL, NULL, 'ANNOUNCEMENT', 2
WHERE NOT EXISTS (SELECT 1 FROM `banner` WHERE `title` = '交易安全提醒');

INSERT INTO `banner` (`title`, `subtitle`, `image`, `link`, `button_text`, `type`, `sort`)
SELECT '新手指引', '3 分钟学会发布第一件闲置物品', NULL,
       '/community', '查看指引', 'GUIDE', 3
WHERE NOT EXISTS (SELECT 1 FROM `banner` WHERE `title` = '新手指引');

SELECT CONCAT('✅ banner 表已就绪，当前运营位 ', COUNT(*), ' 条') AS 结果 FROM `banner`;
