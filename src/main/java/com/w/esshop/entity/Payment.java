package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付流水号 */
    private String payNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private BigDecimal amount;

    /** ALIPAY / WECHAT / MOCK */
    private String payMethod;

    /** PENDING / SUCCESS / FAILED / CLOSED */
    private String status;

    private LocalDateTime payTime;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String productTitle;
}
