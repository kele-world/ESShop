-- ============================================================
-- 增量迁移：模拟支付模块
-- ============================================================
-- 说明：本表记录支付流水。当前为模拟支付（不对接真实支付网关），
--       但数据结构和流程按真实支付的模式设计，后续接入真实
--       支付时只需替换 PaymentService 中的实现。
--
-- 可重复执行，不删除已有数据
-- 执行：mysql -u root -p123456 esshop < migration_payment.sql
-- ============================================================

USE esshop;

CREATE TABLE IF NOT EXISTS `payment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    `pay_no` VARCHAR(40) NOT NULL COMMENT '支付流水号',
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号（冗余，便于查询）',
    `user_id` BIGINT NOT NULL COMMENT '付款人ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_method` VARCHAR(20) NOT NULL DEFAULT 'MOCK' COMMENT '支付方式: ALIPAY-支付宝, WECHAT-微信, MOCK-模拟',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待支付, SUCCESS-支付成功, FAILED-支付失败, CLOSED-已关闭',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付完成时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_no` (`pay_no`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

SELECT '✅ payment 表已就绪' AS 结果;
