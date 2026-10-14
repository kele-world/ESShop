-- ============================================================
-- 增量迁移：商品评价模块
-- ============================================================
-- 可重复执行，不删除已有数据
--
-- 执行：mysql -u root -p123456 esshop < migration_review.sql
-- ============================================================

USE esshop;

CREATE TABLE IF NOT EXISTS `product_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `user_id` BIGINT NOT NULL COMMENT '评价人ID',
    `rating` TINYINT NOT NULL DEFAULT 5 COMMENT '评分 1-5 星',
    `content` VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    -- 一个订单只能评价一次
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

SELECT '✅ product_review 表已就绪' AS 结果;
