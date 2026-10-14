package com.w.esshop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProductStatus {

    ON_SALE("ON_SALE", "在售"),
    SOLD("SOLD", "已售出"),
    OFF_SHELF("OFF_SHELF", "已下架");

    private final String code;
    private final String desc;
}