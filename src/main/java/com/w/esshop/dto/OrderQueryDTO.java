package com.w.esshop.dto;

import lombok.Data;

/**
 * 订单查询
 */
@Data
public class OrderQueryDTO {

    private String status;
    private Long page = 1L;
    private Long size = 10L;
}