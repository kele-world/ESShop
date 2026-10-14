package com.w.esshop.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收银台信息
 */
@Data
public class PayVO {

    private Long paymentId;

    /** 支付流水号 */
    private String payNo;

    private Long orderId;

    private String orderNo;

    /** 商品标题，收银台展示用 */
    private String productTitle;

    /** 实付金额 */
    private BigDecimal amount;

    /** 支付状态 */
    private String status;

    /** 支付方式 */
    private String payMethod;

    /** 待支付剩余秒数（用于前端倒计时） */
    private Long expireSeconds;
}
