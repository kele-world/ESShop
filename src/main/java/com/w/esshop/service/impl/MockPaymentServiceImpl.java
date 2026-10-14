package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.dto.PayVO;
import com.w.esshop.entity.Order;
import com.w.esshop.entity.Payment;
import com.w.esshop.entity.Product;
import com.w.esshop.enums.OrderStatus;
import com.w.esshop.mapper.OrderMapper;
import com.w.esshop.mapper.PaymentMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 模拟支付实现
 *
 * 【重要】这是模拟实现，不会产生任何真实资金流动。
 * 流程完全仿照真实支付：创建支付单 → 用户确认 → 回调更新订单状态。
 * 区别只在于"确认"这一步没有跳转真实网关，而是直接标记成功。
 *
 * 接入真实支付时，替换本类即可：
 *   1. createPayment 改为调用网关下单接口，返回二维码/跳转链接
 *   2. confirmPayment 改为处理网关的异步回调，并验签
 *   3. 其余代码（Controller、前端、订单状态流转）无需改动
 */
@Slf4j
@Service
public class MockPaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;

    /** 支付单有效期：15 分钟 */
    private static final long EXPIRE_MINUTES = 15;

    @Override
    @Transactional
    public PayVO createPayment(Long userId, Long orderId, String payMethod) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权支付此订单");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("该订单当前状态无需支付");
        }

        // 已创建过支付单则直接复用（避免重复下单）
        Payment exist = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        if (exist != null && "SUCCESS".equals(exist.getStatus())) {
            throw new RuntimeException("该订单已支付");
        }

        Payment payment;
        if (exist != null) {
            // 复用旧的待支付单，允许用户换支付方式重试
            payment = exist;
            payment.setPayMethod(payMethod == null ? "MOCK" : payMethod);
            payment.setStatus("PENDING");
            payment.setUpdateTime(LocalDateTime.now());
            paymentMapper.updateById(payment);
        } else {
            payment = new Payment();
            payment.setPayNo(generatePayNo());
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(userId);
            payment.setAmount(order.getAmount());
            payment.setPayMethod(payMethod == null ? "MOCK" : payMethod);
            payment.setStatus("PENDING");
            payment.setCreateTime(LocalDateTime.now());
            payment.setUpdateTime(LocalDateTime.now());
            paymentMapper.insert(payment);
        }

        log.info("[模拟支付] 创建支付单 payNo={}, orderNo={}, amount={}",
                payment.getPayNo(), order.getOrderNo(), order.getAmount());
        return toVO(payment, order);
    }

    @Override
    @Transactional
    public PayVO confirmPayment(Long userId, String payNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPayNo, payNo));
        if (payment == null) {
            throw new RuntimeException("支付单不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此支付单");
        }
        if ("SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException("该支付单已完成，请勿重复支付");
        }

        Order order = orderMapper.selectById(payment.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.PAID.getCode())) {
            throw new RuntimeException("订单状态异常，无法完成支付");
        }

        // === 此处即真实支付中"网关回调验签成功后"的位置 ===
        // 模拟实现：直接标记支付成功
        LocalDateTime now = LocalDateTime.now();

        payment.setStatus("SUCCESS");
        payment.setPayTime(now);
        payment.setUpdateTime(now);
        paymentMapper.updateById(payment);

        order.setStatus(OrderStatus.PAID.getCode());
        order.setUpdateTime(now);
        orderMapper.updateById(order);

        log.info("[模拟支付] 支付成功 payNo={}, orderNo={}", payment.getPayNo(), order.getOrderNo());
        return toVO(payment, order);
    }

    @Override
    public PayVO getByOrderId(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId));
        return payment == null ? null : toVO(payment, order);
    }

    // ==================== 辅助方法 ====================

    /** 生成支付流水号：PAY + 时间戳 + 随机串 */
    private String generatePayNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "PAY" + ts + rand;
    }

    private PayVO toVO(Payment payment, Order order) {
        PayVO vo = new PayVO();
        vo.setPaymentId(payment.getId());
        vo.setPayNo(payment.getPayNo());
        vo.setOrderId(payment.getOrderId());
        vo.setOrderNo(payment.getOrderNo());
        vo.setAmount(payment.getAmount());
        vo.setStatus(payment.getStatus());
        vo.setPayMethod(payment.getPayMethod());

        if (order != null) {
            Product product = productMapper.selectById(order.getProductId());
            vo.setProductTitle(product != null ? product.getTitle() : "商品");
        }

        // 计算剩余有效秒数
        if ("PENDING".equals(payment.getStatus()) && payment.getCreateTime() != null) {
            long elapsed = java.time.Duration.between(payment.getCreateTime(), LocalDateTime.now()).getSeconds();
            long remain = EXPIRE_MINUTES * 60 - elapsed;
            vo.setExpireSeconds(Math.max(0, remain));
        } else {
            vo.setExpireSeconds(0L);
        }
        return vo;
    }
}
