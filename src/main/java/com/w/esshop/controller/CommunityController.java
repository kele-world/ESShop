package com.w.esshop.controller;

import com.w.esshop.common.PageResult;
import com.w.esshop.common.Result;
import com.w.esshop.dto.CommunityPostDTO;
import com.w.esshop.entity.CommunityCategory;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.PostComment;
import com.w.esshop.entity.UserFavorite;
import com.w.esshop.service.CommunityPostService;
import com.w.esshop.service.PostCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "社区模块", description = "社区帖子发布、浏览、点赞、收藏、评论")
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityPostService communityPostService;
    @Autowired
    private PostCommentService commentService;

    // ==================== 帖子 CRUD ====================

    @Operation(summary = "发布帖子")
    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody CommunityPostDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.publish(userId, dto);
        return Result.success("发布成功，等待审核");
    }

    @Operation(summary = "帖子列表")
    @GetMapping("/list")
    public Result<PageResult<CommunityPost>> list(@RequestParam(defaultValue = "1") Long page,
                                                   @RequestParam(defaultValue = "10") Long size,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) String keyword,
                                                   HttpServletRequest request) {
        Long userId = getUserIdOrNull(request);
        return Result.success(communityPostService.page(userId, page, size, categoryId, keyword));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/detail/{id}")
    public Result<CommunityPost> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdOrNull(request);
        return Result.success(communityPostService.detail(userId, id));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.delete(userId, id);
        return Result.success("已删除");
    }

    // ==================== 点赞 ====================

    @Operation(summary = "点赞")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.like(userId, id);
        return Result.success("已点赞");
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.unlike(userId, id);
        return Result.success("已取消");
    }

    // ==================== 收藏 ====================

    @Operation(summary = "收藏")
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.favorite(userId, id);
        return Result.success("已收藏");
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        communityPostService.unfavorite(userId, id);
        return Result.success("已取消");
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/my-favorites")
    public Result<PageResult<UserFavorite>> myFavorites(@RequestParam(defaultValue = "1") Long page,
                                                          @RequestParam(defaultValue = "10") Long size,
                                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(communityPostService.myFavorites(userId, page, size));
    }

    // ==================== 评论 ====================

    @Operation(summary = "评论列表")
    @GetMapping("/{postId}/comments")
    public Result<List<PostComment>> comments(@PathVariable Long postId) {
        return Result.success(commentService.listByPostId(postId));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/{postId}/comment")
    public Result<Void> addComment(@PathVariable Long postId, @RequestBody PostComment comment,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.add(userId, postId, comment.getContent());
        return Result.success("评论成功");
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.delete(userId, commentId);
        return Result.success("已删除");
    }

    // ==================== 分类 ====================

    @Operation(summary = "社区分类列表")
    @GetMapping("/category/list")
    public Result<List<CommunityCategory>> categoryList() {
        return Result.success(communityPostService.categoryList());
    }

    // ==================== 辅助 ====================

    private Long getUserIdOrNull(HttpServletRequest request) {
        try { return (Long) request.getAttribute("userId"); } catch (Exception e) { return null; }
    }
}