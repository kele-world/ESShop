package com.w.esshop.service;

import com.w.esshop.dto.PayVO;

/**
 * 支付服务
 *
 * 当前为模拟支付实现：不对接真实支付网关，在本地完成"创建支付单 → 确认支付"的流程。
 * 后续接入真实支付时，只需新增一个实现类替换本接口的实现，
 * Controller 与前端无需改动。
 */
public interface PaymentService {

    /**
     * 创建支付单（打开收银台）
     *
     * @param payMethod 支付方式：ALIPAY / WECHAT / MOCK
     */
    PayVO createPayment(Long userId, Long orderId, String payMethod);

    /**
     * 确认支付（模拟支付成功）
     *
     * @return 更新后的支付信息
     */
    PayVO confirmPayment(Long userId, String payNo);

    /**
     * 查询某订单的支付信息（用于收银台回显）
     */
    PayVO getByOrderId(Long userId, Long orderId);
}
