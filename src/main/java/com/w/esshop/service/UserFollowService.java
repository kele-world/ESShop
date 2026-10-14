package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.entity.User;

/**
 * 用户关注服务
 */
public interface UserFollowService {

    /** 关注 */
    void follow(Long followerId, Long followingId);

    /** 取消关注 */
    void unfollow(Long followerId, Long followingId);

    /** 是否已关注 */
    boolean isFollowing(Long followerId, Long followingId);

    /** 粉丝数 */
    long followerCount(Long userId);

    /** 关注数 */
    long followingCount(Long userId);

    /** 我的粉丝列表 */
    PageResult<User> followers(Long userId, Long page, Long size);

    /** 我关注的人 */
    PageResult<User> followings(Long userId, Long page, Long size);
}
