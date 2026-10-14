package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.UserFavorite;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.UserFavoriteMapper;
import com.w.esshop.service.ProductFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    @Autowired private UserFavoriteMapper favoriteMapper;
    @Autowired private ProductMapper productMapper;

    @Override
    @Transactional
    public void favorite(Long userId, Long productId) {
        // 已收藏过则直接返回，避免重复计数
        if (favoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId).eq(UserFavorite::getTargetId, productId)
                .eq(UserFavorite::getType, "PRODUCT")) > 0) return;
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId); fav.setTargetId(productId); fav.setType("PRODUCT");
        fav.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(fav);
        // 收藏数 +1
        Product product = productMapper.selectById(productId);
        if (product != null) {
            int count = product.getFavoriteCount() == null ? 0 : product.getFavoriteCount();
            product.setFavoriteCount(count + 1);
            productMapper.updateById(product);
        }
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long productId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId).eq(UserFavorite::getTargetId, productId)
                .eq(UserFavorite::getType, "PRODUCT"));
        // 只有确实删掉了记录才减计数，避免重复点导致减多了
        if (deleted > 0) {
            Product product = productMapper.selectById(productId);
            if (product != null && product.getFavoriteCount() != null && product.getFavoriteCount() > 0) {
                product.setFavoriteCount(product.getFavoriteCount() - 1);
                productMapper.updateById(product);
            }
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId).eq(UserFavorite::getTargetId, productId)
                .eq(UserFavorite::getType, "PRODUCT")) > 0;
    }

    @Override
    public PageResult<UserFavorite> myFavorites(Long userId, Long pageNum, Long size) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId).eq(UserFavorite::getType, "PRODUCT");
        wrapper.orderByDesc(UserFavorite::getCreateTime);
        Page<UserFavorite> page = new Page<>(pageNum, size);
        Page<UserFavorite> result = favoriteMapper.selectPage(page, wrapper);
        if (!result.getRecords().isEmpty()) {
            List<Long> productIds = result.getRecords().stream().map(UserFavorite::getTargetId).toList();
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
            result.getRecords().forEach(f -> {
                Product p = productMap.get(f.getTargetId());
                if (p != null) { f.setTargetTitle(p.getTitle()); f.setTargetImage(p.getImages()); }
            });
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }
}