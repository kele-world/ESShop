package com.w.esshop.controller;

import com.w.esshop.common.PageResult;
import com.w.esshop.common.Result;
import com.w.esshop.dto.OrderQueryDTO;
import com.w.esshop.dto.ProductQueryDTO;
import com.w.esshop.entity.Banner;
import com.w.esshop.entity.Category;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.Order;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.User;
import com.w.esshop.service.BannerService;
import com.w.esshop.service.CategoryService;
import com.w.esshop.service.CommunityPostService;
import com.w.esshop.service.OrderService;
import com.w.esshop.service.ProductService;
import com.w.esshop.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 —— 所有业务逻辑通过 Service 层完成，Controller 不直接依赖 Mapper
 */
@Tag(name = "管理后台", description = "管理员专用接口：用户、商品、订单、分类管理")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CommunityPostService communityPostService;
    @Autowired
    private BannerService bannerService;

    /** 校验管理员权限 */
    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new RuntimeException("无管理员权限");
        }
    }

    // ==================== 仪表盘 ====================

    @Operation(summary = "仪表盘数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(Map.of(
                "userCount",           userService.countUsers(),
                "productCount",        productService.countOnSale(),
                "orderCount",          orderService.countOrders(),
                "completedOrderCount", orderService.countCompletedOrders()
        ));
    }

    // ==================== 用户管理 ====================

    @Operation(summary = "用户列表")
    @GetMapping("/user/list")
    public Result<PageResult<User>> userList(HttpServletRequest request,
                                              @RequestParam(defaultValue = "1") Long page,
                                              @RequestParam(defaultValue = "10") Long size,
                                              @RequestParam(required = false) String keyword) {
        checkAdmin(request);
        return Result.success(userService.adminPageUsers(page, size, keyword));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/user/{id}/status")
    public Result<Void> updateUserStatus(HttpServletRequest request,
                                          @PathVariable Long id,
                                          @RequestBody Map<String, Integer> body) {
        checkAdmin(request);
        userService.adminUpdateStatus(id, body.get("status"));
        return Result.success("更新成功");
    }

    // ==================== 商品管理 ====================

    @Operation(summary = "商品列表（管理员）")
    @GetMapping("/product/list")
    public Result<PageResult<Product>> productList(HttpServletRequest request, ProductQueryDTO query) {
        checkAdmin(request);
        query.setStatus(null);
        return Result.success(productService.page(query));
    }

    @Operation(summary = "删除商品（管理员）")
    @DeleteMapping("/product/{id}")
    public Result<Void> deleteProduct(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        productService.delete(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "强制下架商品（管理员）")
    @PutMapping("/product/{id}/off-shelf")
    public Result<Void> forceOffShelf(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        productService.adminOffShelf(id);
        return Result.success("已下架");
    }

    // ==================== 订单管理 ====================

    @Operation(summary = "订单列表（管理员）")
    @GetMapping("/order/list")
    public Result<PageResult<Order>> orderList(HttpServletRequest request, OrderQueryDTO query) {
        checkAdmin(request);
        return Result.success(orderService.adminPageOrders(query));
    }

    // ==================== 分类管理 ====================

    @Operation(summary = "新增分类")
    @PostMapping("/category")
    public Result<Void> addCategory(HttpServletRequest request, @RequestBody Category category) {
        checkAdmin(request);
        categoryService.add(category);
        return Result.success("添加成功");
    }

    @Operation(summary = "更新分类")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(HttpServletRequest request,
                                        @PathVariable Long id,
                                        @RequestBody Category category) {
        checkAdmin(request);
        category.setId(id);
        categoryService.update(category);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        categoryService.delete(id);
        return Result.success("删除成功");
    }

    // ==================== 社区管理 ====================

    @Operation(summary = "待审核帖子列表")
    @GetMapping("/community/pending")
    public Result<PageResult<CommunityPost>> communityPending(HttpServletRequest request,
                                                               @RequestParam(defaultValue = "1") Long page,
                                                               @RequestParam(defaultValue = "10") Long size) {
        checkAdmin(request);
        return Result.success(communityPostService.adminPendingPage(page, size));
    }

    @Operation(summary = "全部帖子列表")
    @GetMapping("/community/all")
    public Result<PageResult<CommunityPost>> communityAll(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "1") Long page,
                                                           @RequestParam(defaultValue = "10") Long size) {
        checkAdmin(request);
        return Result.success(communityPostService.adminAllPage(page, size));
    }

    @Operation(summary = "已下架帖子列表")
    @GetMapping("/community/off-shelf")
    public Result<PageResult<CommunityPost>> communityOffShelf(HttpServletRequest request,
                                                               @RequestParam(defaultValue = "1") Long page,
                                                               @RequestParam(defaultValue = "10") Long size) {
        checkAdmin(request);
        return Result.success(communityPostService.adminOffShelfPage(page, size));
    }

    @Operation(summary = "审核通过")
    @PutMapping("/community/{id}/approve")
    public Result<Void> communityApprove(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        communityPostService.approve(id);
        return Result.success("已通过");
    }

    @Operation(summary = "审核拒绝")
    @PutMapping("/community/{id}/reject")
    public Result<Void> communityReject(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        communityPostService.reject(id);
        return Result.success("已拒绝");
    }

    @Operation(summary = "下架帖子")
    @PutMapping("/community/{id}/off-shelf")
    public Result<Void> communityOffShelfPost(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        communityPostService.adminOffShelf(id);
        return Result.success("已下架");
    }

    @Operation(summary = "重新上架")
    @PutMapping("/community/{id}/re-list")
    public Result<Void> communityReList(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        communityPostService.adminReList(id);
        return Result.success("已重新上架");
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/community/{id}")
    public Result<Void> communityDelete(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        communityPostService.adminDelete(id);
        return Result.success("已删除");
    }

    // ==================== 运营位管理 ====================

    @Operation(summary = "运营位列表（含已停用/过期）")
    @GetMapping("/banner/list")
    public Result<List<Banner>> bannerList(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(bannerService.listAll());
    }

    @Operation(summary = "新增运营位")
    @PostMapping("/banner")
    public Result<Void> addBanner(HttpServletRequest request, @RequestBody Banner banner) {
        checkAdmin(request);
        bannerService.add(banner);
        return Result.success("添加成功");
    }

    @Operation(summary = "更新运营位")
    @PutMapping("/banner/{id}")
    public Result<Void> updateBanner(HttpServletRequest request,
                                      @PathVariable Long id,
                                      @RequestBody Banner banner) {
        checkAdmin(request);
        banner.setId(id);
        bannerService.update(banner);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除运营位")
    @DeleteMapping("/banner/{id}")
    public Result<Void> deleteBanner(HttpServletRequest request, @PathVariable Long id) {
        checkAdmin(request);
        bannerService.delete(id);
        return Result.success("删除成功");
    }
}