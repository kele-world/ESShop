package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.dto.OrderDTO;
import com.w.esshop.dto.OrderQueryDTO;
import com.w.esshop.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService {

    /** 创建订单 */
    Order create(Long buyerId, OrderDTO dto);

    /** 我的订单 */
    PageResult<Order> pageMyOrders(Long userId, OrderQueryDTO query);

    /** 我卖出的订单 */
    PageResult<Order> pageMySold(Long userId, OrderQueryDTO query);

    /** 订单详情 */
    Order detail(Long orderId);

    /** 付款 */
    void pay(Long userId, Long orderId);

    /** 完成订单 */
    void complete(Long userId, Long orderId);

    /** 取消订单 */
    void cancel(Long userId, Long orderId);

    /** 删除订单（仅已取消/已完成的订单可删，软删除不物理删除） */
    void deleteOrder(Long userId, Long orderId);

    // ==================== 管理员方法 ====================

    /** 分页查询所有订单(管理员) */
    PageResult<Order> adminPageOrders(OrderQueryDTO query);

    /** 统计订单总数 */
    long countOrders();

    /** 统计已完成订单数 */
    long countCompletedOrders();
}