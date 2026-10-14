package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价
 */
@Data
@TableName("product_review")
public class ProductReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long orderId;

    /** 评价人ID（买家） */
    private Long userId;

    /** 被评价人ID（卖家）—— 评价对象 */
    private Long sellerId;

    /** 评分 1-5 星 */
    private Integer rating;

    private String content;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String productTitle;
}
