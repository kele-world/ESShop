package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.UserProfileVO;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.Product;

/**
 * 用户主页服务
 */
public interface UserProfileService {

    /**
     * 获取用户主页信息
     *
     * @param userId        要查看的用户
     * @param currentUserId 当前登录用户，用于判断是否已关注；未登录传 null
     */
    UserProfileVO getProfile(Long userId, Long currentUserId);

    /** 该用户发布的商品 */
    PageResult<Product> products(Long userId, Long page, Long size);

    /** 该用户发布的社区帖子（仅已审核通过） */
    PageResult<CommunityPost> posts(Long userId, Long page, Long size);

    /** 该用户收到的评价 */
    ProductReviewVO reviews(Long userId, Long page, Long size);
}
