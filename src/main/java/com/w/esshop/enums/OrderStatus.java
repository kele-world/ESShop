package com.w.esshop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    PENDING_PAYMENT("PENDING_PAYMENT", "待付款"),
    PAID("PAID", "已付款"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    /**
     * 状态流转校验
     */
    public static boolean canTransition(String from, String to) {
        return switch (from) {
            case "PENDING_PAYMENT" -> "PAID".equals(to) || "CANCELLED".equals(to);
            case "PAID" -> "COMPLETED".equals(to) || "CANCELLED".equals(to);
            case "COMPLETED", "CANCELLED" -> false;
            default -> false;
        };
    }
}