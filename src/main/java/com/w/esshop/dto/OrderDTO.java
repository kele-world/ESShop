package com.w.esshop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 下单请求
 */
@Data
public class OrderDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private String remark;
}