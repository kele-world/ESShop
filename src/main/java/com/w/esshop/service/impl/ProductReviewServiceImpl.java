package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.ReviewDTO;
import com.w.esshop.entity.Order;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.ProductReview;
import com.w.esshop.entity.User;
import com.w.esshop.mapper.OrderMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.ProductReviewMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卖家评价实现
 *
 * 评价对象是「卖家」而非商品：二手商品一件只卖一次，
 * 商品维度的评价没有参考价值，卖家信用才有意义。
 */
@Service
public class ProductReviewServiceImpl implements ProductReviewService {

    @Autowired
    private ProductReviewMapper reviewMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional
    public void submit(Long userId, ReviewDTO dto) {
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 只有买家能评价自己的订单
        if (!order.getBuyerId().equals(userId)) {
            throw new RuntimeException("只能评价自己购买的订单");
        }
        // 只有已完成的订单才能评价
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("订单完成后才能评价");
        }
        // 一个订单只能评价一次
        if (hasReviewed(dto.getOrderId())) {
            throw new RuntimeException("该订单已评价过了");
        }

        ProductReview review = new ProductReview();
        review.setProductId(order.getProductId());   // 记录交易背景
        review.setOrderId(order.getId());
        review.setUserId(userId);                    // 评价人 = 买家
        review.setSellerId(order.getSellerId());     // 被评价人 = 卖家
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        reviewMapper.insert(review);
    }

    @Override
    public ProductReviewVO listBySeller(Long sellerId, Long pageNum, Long size) {
        Page<ProductReview> page = new Page<>(pageNum, size);
        Page<ProductReview> result = reviewMapper.selectPage(page,
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getSellerId, sellerId)
                        .orderByDesc(ProductReview::getCreateTime));

        List<ProductReview> records = result.getRecords();
        if (!records.isEmpty()) {
            // 补上评价人（买家）的头像昵称
            List<Long> userIds = records.stream().map(ProductReview::getUserId).distinct().toList();
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            // 补上交易的商品标题，让评价有上下文（"买了他的XX"）
            List<Long> productIds = records.stream().map(ProductReview::getProductId)
                    .filter(id -> id != null).distinct().toList();
            Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                    : productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

            records.forEach(r -> {
                User u = userMap.get(r.getUserId());
                if (u != null) {
                    r.setUserName(u.getNickname());
                    r.setUserAvatar(u.getAvatar());
                }
                Product p = productMap.get(r.getProductId());
                if (p != null) {
                    r.setProductTitle(p.getTitle());
                }
            });
        }

        ProductReviewVO vo = new ProductReviewVO();
        vo.setAvgRating(avgRating(sellerId));
        vo.setTotal(result.getTotal());
        vo.setReviews(records.stream().map(r -> {
            ProductReviewVO.Item item = new ProductReviewVO.Item();
            item.setId(r.getId());
            item.setUserId(r.getUserId());
            item.setUserName(r.getUserName());
            item.setUserAvatar(r.getUserAvatar());
            item.setRating(r.getRating());
            item.setContent(r.getContent());
            item.setProductTitle(r.getProductTitle());
            item.setCreateTime(r.getCreateTime() != null ? r.getCreateTime().toString() : null);
            return item;
        }).toList());

        return vo;
    }

    @Override
    public boolean hasReviewed(Long orderId) {
        return reviewMapper.selectCount(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getOrderId, orderId)) > 0;
    }

    @Override
    public Double avgRating(Long sellerId) {
        List<ProductReview> all = reviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>().eq(ProductReview::getSellerId, sellerId));
        if (all.isEmpty()) {
            return 0.0;
        }
        double sum = all.stream().mapToInt(ProductReview::getRating).sum();
        // 保留 1 位小数
        return Math.round(sum / all.size() * 10) / 10.0;
    }

    @Override
    public long reviewCount(Long sellerId) {
        return reviewMapper.selectCount(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getSellerId, sellerId));
    }
}
