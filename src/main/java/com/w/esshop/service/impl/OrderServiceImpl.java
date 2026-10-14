package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.dto.OrderDTO;
import com.w.esshop.dto.OrderQueryDTO;
import com.w.esshop.entity.Order;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.ProductReview;
import com.w.esshop.entity.User;
import com.w.esshop.enums.OrderStatus;
import com.w.esshop.mapper.OrderMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.ProductReviewMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductReviewMapper reviewMapper;

    @Override
    @Transactional
    public Order create(Long buyerId, OrderDTO dto) {
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架或已售出");
        }
        if (product.getUserId().equals(buyerId)) {
            throw new RuntimeException("不能购买自己的商品");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProductId(product.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setAmount(product.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setRemark(dto.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        product.setStatus("SOLD");
        productMapper.updateById(product);

        return order;
    }

    @Override
    public PageResult<Order> pageMyOrders(Long userId, OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, userId);
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(Order::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(query.getPage(), query.getSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        fillOrderInfo(result.getRecords());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResult<Order> pageMySold(Long userId, OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, userId);
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(Order::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = new Page<>(query.getPage(), query.getSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        fillOrderInfo(result.getRecords());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Order detail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        fillOrderInfo(List.of(order));
        return order;
    }

    @Override
    @Transactional
    public void pay(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.PAID.getCode())) {
            throw new RuntimeException("当前订单状态不允许付款");
        }
        order.setStatus(OrderStatus.PAID.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void complete(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getSellerId().equals(userId) && !order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.COMPLETED.getCode())) {
            throw new RuntimeException("当前订单状态不允许完成");
        }
        order.setStatus(OrderStatus.COMPLETED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.CANCELLED.getCode())) {
            throw new RuntimeException("当前订单状态不允许取消");
        }
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null && "SOLD".equals(product.getStatus())) {
            product.setStatus("ON_SALE");
            productMapper.updateById(product);
        }
    }

    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权删除此订单");
        }
        // 进行中的订单不允许删除，避免丢失交易凭证
        if ("PENDING_PAYMENT".equals(order.getStatus()) || "PAID".equals(order.getStatus())) {
            throw new RuntimeException("进行中的订单不能删除，请先取消订单");
        }
        // 逻辑删除：仅对用户隐藏，数据仍保留在库中，便于管理员追溯
        orderMapper.deleteById(orderId);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "ES" + timestamp + uuid;
    }

    private void fillOrderInfo(List<Order> orders) {
        if (orders.isEmpty()) return;

        List<Long> productIds = orders.stream().map(Order::getProductId).distinct().toList();
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> userIds = orders.stream().flatMap(o -> java.util.stream.Stream.of(o.getBuyerId(), o.getSellerId())).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 查询哪些订单已被评价过（用于前端显示"评价"按钮）
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Set<Long> reviewedOrderIds = reviewMapper
                .selectList(new LambdaQueryWrapper<ProductReview>().in(ProductReview::getOrderId, orderIds))
                .stream().map(ProductReview::getOrderId).collect(Collectors.toSet());

        orders.forEach(o -> {
            Product p = productMap.get(o.getProductId());
            if (p != null) {
                o.setProductTitle(p.getTitle());
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    o.setProductImage(p.getImages().split(",")[0]);
                }
            }
            User buyer = userMap.get(o.getBuyerId());
            if (buyer != null) o.setBuyerName(buyer.getNickname());
            User seller = userMap.get(o.getSellerId());
            if (seller != null) o.setSellerName(seller.getNickname());
            o.setReviewed(reviewedOrderIds.contains(o.getId()));
        });
    }

    // ==================== 管理员方法 ====================

    @Override
    public PageResult<Order> adminPageOrders(OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(Order::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = new Page<>(query.getPage(), query.getSize());
        Page<Order> result = orderMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public long countOrders() {
        return orderMapper.selectCount(null);
    }

    @Override
    public long countCompletedOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "COMPLETED");
        return orderMapper.selectCount(wrapper);
    }
}