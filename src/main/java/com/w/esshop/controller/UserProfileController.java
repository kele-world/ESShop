package com.w.esshop.controller;

import com.w.esshop.common.PageResult;
import com.w.esshop.common.Result;
import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.UserProfileVO;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.User;
import com.w.esshop.service.UserFollowService;
import com.w.esshop.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户主页控制器
 *
 * 主页聚合三类内容，对应前端三个 Tab：
 *   - 商品：他发布的闲置
 *   - 社区：他发布的帖子
 *   - 评价：他作为卖家收到的评价
 *
 * 该接口对游客开放（软鉴权），登录用户能额外看到「是否已关注」。
 */
@Tag(name = "用户主页", description = "用户资料、商品、帖子、评价、关注")
@RestController
@RequestMapping("/api/user-profile")
public class UserProfileController {

    @Autowired
    private UserProfileService profileService;
    @Autowired
    private UserFollowService followService;

    @Operation(summary = "用户主页信息")
    @GetMapping("/{userId}")
    public Result<UserProfileVO> profile(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        return Result.success(profileService.getProfile(userId, currentUserId));
    }

    @Operation(summary = "他发布的商品")
    @GetMapping("/{userId}/products")
    public Result<PageResult<Product>> products(@PathVariable Long userId,
                                                 @RequestParam(defaultValue = "1") Long page,
                                                 @RequestParam(defaultValue = "12") Long size) {
        return Result.success(profileService.products(userId, page, size));
    }

    @Operation(summary = "他发布的帖子")
    @GetMapping("/{userId}/posts")
    public Result<PageResult<CommunityPost>> posts(@PathVariable Long userId,
                                                    @RequestParam(defaultValue = "1") Long page,
                                                    @RequestParam(defaultValue = "12") Long size) {
        return Result.success(profileService.posts(userId, page, size));
    }

    @Operation(summary = "他收到的评价")
    @GetMapping("/{userId}/reviews")
    public Result<ProductReviewVO> reviews(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "1") Long page,
                                            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(profileService.reviews(userId, page, size));
    }

    // ==================== 关注 ====================

    @Operation(summary = "关注")
    @PostMapping("/{userId}/follow")
    public Result<Void> follow(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        followService.follow(currentUserId, userId);
        return Result.success("已关注");
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{userId}/follow")
    public Result<Void> unfollow(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        followService.unfollow(currentUserId, userId);
        return Result.success("已取消关注");
    }

    @Operation(summary = "他的粉丝")
    @GetMapping("/{userId}/followers")
    public Result<PageResult<User>> followers(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "1") Long page,
                                               @RequestParam(defaultValue = "20") Long size) {
        return Result.success(followService.followers(userId, page, size));
    }

    @Operation(summary = "他关注的人")
    @GetMapping("/{userId}/followings")
    public Result<PageResult<User>> followings(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "20") Long size) {
        return Result.success(followService.followings(userId, page, size));
    }
}
