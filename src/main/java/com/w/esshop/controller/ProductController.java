package com.w.esshop.controller;

import com.w.esshop.common.PageResult;
import com.w.esshop.common.Result;
import com.w.esshop.dto.ProductDTO;
import com.w.esshop.dto.ProductQueryDTO;
import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.ReviewDTO;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.UserFavorite;
import com.w.esshop.service.ProductFavoriteService;
import com.w.esshop.service.ProductReviewService;
import com.w.esshop.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 商品控制器
 */
@Tag(name = "商品模块", description = "商品发布、查询、编辑、下架")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductFavoriteService favoriteService;
    @Autowired
    private ProductReviewService reviewService;
    @Autowired
    private com.w.esshop.client.AiChatClient aiChatClient;

    @Operation(summary = "发布商品")
    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody ProductDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.publish(userId, dto);
        return Result.success("发布成功");
    }

    @Operation(summary = "商品列表（分页、分类筛选、关键词搜索）")
    @GetMapping("/list")
    public Result<PageResult<Product>> list(ProductQueryDTO query) {
        return Result.success(productService.page(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result<Product> detail(@PathVariable Long id, HttpServletRequest request) {
        // 该接口在拦截器白名单中，未登录时 userId 为 null
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(productService.detail(id, userId));
    }

    @Operation(summary = "编辑商品")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.update(userId, id, dto);
        return Result.success("更新成功");
    }

    @Operation(summary = "快捷改价")
    @PutMapping("/{id}/price")
    public Result<Void> changePrice(@PathVariable Long id, @RequestBody ProductDTO dto,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.changePrice(userId, id, dto.getPrice());
        return Result.success("改价成功");
    }

    @Operation(summary = "下架商品")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.offShelf(userId, id);
        return Result.success("已下架");
    }

    @Operation(summary = "重新上架")
    @PutMapping("/{id}/re-shelf")
    public Result<Void> reShelf(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.reShelf(userId, id);
        return Result.success("已重新上架");
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        productService.deleteMyProduct(userId, id);
        return Result.success("已删除");
    }

    @Operation(summary = "我发布的商品")
    @GetMapping("/my")
    public Result<PageResult<Product>> myProducts(ProductQueryDTO query, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        query.setUserId(userId);
        query.setStatus(null);
        return Result.success(productService.page(query));
    }

    // ==================== 收藏 ====================

    @Operation(summary = "收藏商品")
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.favorite(userId, id);
        return Result.success("已收藏");
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.unfavorite(userId, id);
        return Result.success("已取消");
    }

    @Operation(summary = "我的商品收藏")
    @GetMapping("/my-favorites")
    public Result<PageResult<UserFavorite>> myFavorites(@RequestParam(defaultValue = "1") Long page,
                                                          @RequestParam(defaultValue = "10") Long size,
                                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.myFavorites(userId, page, size));
    }

    // ==================== 卖家评价 ====================
    // 注：评价对象是卖家，不是商品。入口在订单页（交易完成后评价卖家）
    //     查询接口见 UserProfileController 的 /api/user-profile/{id}/reviews

    @Operation(summary = "提交卖家评价")
    @PostMapping("/review")
    public Result<Void> submitReview(@Valid @RequestBody ReviewDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reviewService.submit(userId, dto);
        return Result.success("评价成功");
    }

    // ==================== 语义搜索 ====================

    @Operation(summary = "自然语言搜索商品",
               description = "用户描述需求（如「有没有便宜的代步工具」），由语义检索匹配商品")
    @GetMapping("/semantic-search")
    public Result<PageResult<Product>> semanticSearch(@RequestParam String query,
                                                       @RequestParam(defaultValue = "12") Long size) {
        // 交给 Python 侧做向量检索，拿到商品ID列表
        List<Long> ids = aiChatClient.searchProducts(query, size.intValue());

        if (ids.isEmpty()) {
            // 客服服务不可用或无结果 —— 前端会回退到普通关键词搜索
            return Result.success(PageResult.of(0L, 1L, size, List.of()));
        }

        // 按检索出的顺序返回商品详情（保持相关性排序）
        List<Product> products = ids.stream()
                .map(productService::detailForSearch)
                .filter(Objects::nonNull)
                .toList();

        return Result.success(PageResult.of((long) products.size(), 1L, size, products));
    }
}