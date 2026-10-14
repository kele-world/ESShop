package com.w.esshop.dto;

import lombok.Data;

/**
 * 用户主页信息
 */
@Data
public class UserProfileVO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    /** 卖家平均评分 */
    private Double avgRating;

    /** 收到的评价数 */
    private Long reviewCount;

    /** 已发布商品数 */
    private Long productCount;

    /** 已发布帖子数 */
    private Long postCount;

    /** 粉丝数 */
    private Long followerCount;

    /** 关注数 */
    private Long followingCount;

    /** 当前登录用户是否已关注他（未登录为 null） */
    private Boolean following;

    /** 是否是自己的主页 */
    private Boolean self;
}
