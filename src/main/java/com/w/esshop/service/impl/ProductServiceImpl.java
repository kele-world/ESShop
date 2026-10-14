package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.dto.ProductDTO;
import com.w.esshop.dto.ProductQueryDTO;
import com.w.esshop.entity.Category;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.User;
import com.w.esshop.entity.UserFavorite;
import com.w.esshop.mapper.CategoryMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.UserFavoriteMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserFavoriteMapper favoriteMapper;

    @Override
    public void publish(Long userId, ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setUserId(userId);
        product.setStatus("ON_SALE");
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
    }

    @Override
    public PageResult<Product> page(ProductQueryDTO query) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Product::getTitle, query.getKeyword());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Product::getStatus, query.getStatus());
        } else if (query.getUserId() == null) {
            // 无 userId（浏览商品）时默认只看在售；查"我的商品"时展示所有状态
            wrapper.eq(Product::getStatus, "ON_SALE");
        }
        if (query.getUserId() != null) {
            wrapper.eq(Product::getUserId, query.getUserId());
        }

        if ("price".equals(query.getSort())) {
            wrapper.orderBy(true, "asc".equals(query.getOrder()), Product::getPrice);
        } else if ("view_count".equals(query.getSort())) {
            wrapper.orderBy(true, "asc".equals(query.getOrder()), Product::getViewCount);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        Page<Product> page = new Page<>(query.getPage(), query.getSize());
        Page<Product> result = productMapper.selectPage(page, wrapper);

        if (!result.getRecords().isEmpty()) {
            List<Long> categoryIds = result.getRecords().stream()
                    .map(Product::getCategoryId).distinct().toList();
            Map<Long, String> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            List<Long> userIds = result.getRecords().stream()
                    .map(Product::getUserId).distinct().toList();
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            result.getRecords().forEach(p -> {
                p.setCategoryName(categoryMap.getOrDefault(p.getCategoryId(), "未知"));
                User seller = userMap.get(p.getUserId());
                if (seller != null) {
                    p.setSellerName(seller.getNickname());
                    p.setSellerAvatar(seller.getAvatar());
                }
            });
        }

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Product detail(Long id, Long currentUserId) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setViewCount(product.getViewCount() + 1);
        productMapper.updateById(product);

        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            product.setCategoryName(category.getName());
        }
        User seller = userMapper.selectById(product.getUserId());
        if (seller != null) {
            product.setSellerName(seller.getNickname());
            product.setSellerAvatar(seller.getAvatar());
        }
        // 标记当前用户是否已收藏（未登录时不设置，前端按 false 处理）
        if (currentUserId != null) {
            product.setFavorited(favoriteMapper.selectCount(
                    new LambdaQueryWrapper<UserFavorite>()
                            .eq(UserFavorite::getUserId, currentUserId)
                            .eq(UserFavorite::getTargetId, id)
                            .eq(UserFavorite::getType, "PRODUCT")) > 0);
        }
        return product;
    }

    @Override
    public Product detailForSearch(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return null;
        }
        // 与 detail 不同：这里不增加浏览量，也不查收藏状态。
        // 语义搜索的结果列表只是展示，点进去才应该计一次浏览。
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            product.setCategoryName(category.getName());
        }
        User seller = userMapper.selectById(product.getUserId());
        if (seller != null) {
            product.setSellerName(seller.getNickname());
            product.setSellerAvatar(seller.getAvatar());
        }
        return product;
    }

    @Override
    public void update(Long userId, Long productId, ProductDTO dto) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改他人商品");
        }
        BeanUtils.copyProperties(dto, product);
        product.setId(productId);
        product.setUserId(userId);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    public void changePrice(Long userId, Long productId, BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("价格不合法");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改他人商品");
        }
        product.setPrice(price);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    public void offShelf(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人商品");
        }
        product.setStatus("OFF_SHELF");
        productMapper.updateById(product);
    }

    @Override
    public void reShelf(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作他人商品");
        }
        if (!"OFF_SHELF".equals(product.getStatus())) {
            throw new RuntimeException("仅已下架的商品可以重新上架");
        }
        product.setStatus("ON_SALE");
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    public void delete(Long productId) {
        productMapper.deleteById(productId);
    }

    @Override
    public void deleteMyProduct(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除他人商品");
        }
        productMapper.deleteById(productId);
    }

    @Override
    public void adminOffShelf(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        product.setStatus("OFF_SHELF");
        productMapper.updateById(product);
    }

    @Override
    public long countOnSale() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, "ON_SALE");
        return productMapper.selectCount(wrapper);
    }
}