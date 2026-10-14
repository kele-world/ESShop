package com.w.esshop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布/编辑商品请求
 */
@Data
public class ProductDTO {

    @NotBlank(message = "商品标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 商品图片，多个用逗号分隔。二手交易靠图看货，必填 */
    @NotBlank(message = "请至少上传一张商品图片")
    private String images;

    @NotNull(message = "分类不能为空")
    private Long categoryId;
}