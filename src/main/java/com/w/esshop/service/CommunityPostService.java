package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.dto.CommunityPostDTO;
import com.w.esshop.entity.CommunityCategory;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.UserFavorite;

import java.util.List;

public interface CommunityPostService {

    void publish(Long userId, CommunityPostDTO dto);

    PageResult<CommunityPost> page(Long userId, Long page, Long size, Long categoryId, String keyword);

    /** 某个用户发布的帖子（仅已审核通过），用于用户主页 */
    PageResult<CommunityPost> pageOfUser(Long userId, Long page, Long size);

    CommunityPost detail(Long userId, Long id);

    void delete(Long userId, Long postId);

    // 点赞
    void like(Long userId, Long postId);
    void unlike(Long userId, Long postId);

    // 收藏
    void favorite(Long userId, Long postId);
    void unfavorite(Long userId, Long postId);
    PageResult<UserFavorite> myFavorites(Long userId, Long page, Long size);

    // 分类
    List<CommunityCategory> categoryList();

    // 管理员
    PageResult<CommunityPost> adminPendingPage(Long page, Long size);
    PageResult<CommunityPost> adminAllPage(Long page, Long size);
    PageResult<CommunityPost> adminOffShelfPage(Long page, Long size);
    void approve(Long postId);
    void reject(Long postId);
    void adminOffShelf(Long postId);
    void adminReList(Long postId);
    void adminDelete(Long postId);
}