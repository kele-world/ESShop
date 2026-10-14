package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_info")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal amount;

    /** 订单状态: PENDING_PAYMENT-待付款, PAID-已付款, COMPLETED-已完成, CANCELLED-已取消 */
    private String status;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String productTitle;

    @TableField(exist = false)
    private String productImage;

    @TableField(exist = false)
    private String buyerName;

    @TableField(exist = false)
    private String sellerName;

    /** 当前买家是否已评价（仅订单列表查询时填充） */
    @TableField(exist = false)
    private Boolean reviewed;
}