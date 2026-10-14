package com.w.esshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交评价请求
 */
@Data
public class ReviewDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低1星")
    @Max(value = 5, message = "评分最高5星")
    private Integer rating;

    private String content;
}
