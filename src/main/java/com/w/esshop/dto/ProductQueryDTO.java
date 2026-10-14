package com.w.esshop.dto;

import lombok.Data;

/**
 * 商品查询请求
 */
@Data
public class ProductQueryDTO {

    private Long categoryId;
    private String keyword;
    private String status;
    private Long userId;
    private Long page = 1L;
    private Long size = 12L;
    private String sort = "create_time";  // create_time, price, view_count
    private String order = "desc";        // asc, desc
}