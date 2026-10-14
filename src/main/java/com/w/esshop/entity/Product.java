package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    /** 商品图片，多个用逗号分隔 */
    private String images;

    private Long categoryId;

    private Long userId;

    /** 状态: ON_SALE-在售, SOLD-已售出, OFF_SHELF-已下架 */
    private String status;

    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String categoryName;

    /** 当前登录用户是否已收藏该商品（未登录为 null） */
    @TableField(exist = false)
    private Boolean favorited;

    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String sellerAvatar;
}