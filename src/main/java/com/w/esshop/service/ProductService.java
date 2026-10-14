package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.dto.ProductDTO;
import com.w.esshop.dto.ProductQueryDTO;
import com.w.esshop.entity.Product;

/**
 * 商品服务接口
 */
public interface ProductService {

    /** 发布商品 */
    void publish(Long userId, ProductDTO dto);

    /** 分页查询商品 */
    PageResult<Product> page(ProductQueryDTO query);

    /**
     * 商品详情
     *
     * @param currentUserId 当前登录用户ID，用于标记 favorited；未登录传 null
     */
    Product detail(Long id, Long currentUserId);

    /**
     * 按 ID 取商品（用于语义搜索结果回填）
     *
     * 与 detail 的区别：不增加浏览次数，也不查收藏状态。
     * 搜索结果列表里点开才该计数，列表本身不应影响浏览量。
     */
    Product detailForSearch(Long id);

    /** 编辑商品 */
    void update(Long userId, Long productId, ProductDTO dto);

    /** 快捷改价 */
    void changePrice(Long userId, Long productId, java.math.BigDecimal price);

    /** 下架商品 */
    void offShelf(Long userId, Long productId);

    /** 重新上架(用户自己的已下架商品) */
    void reShelf(Long userId, Long productId);

    /** 删除商品(管理员) */
    void delete(Long productId);

    /** 删除自己的商品 */
    void deleteMyProduct(Long userId, Long productId);

    /** 强制下架(管理员，无需校验归属) */
    void adminOffShelf(Long productId);

    /** 统计在售商品数 */
    long countOnSale();
}