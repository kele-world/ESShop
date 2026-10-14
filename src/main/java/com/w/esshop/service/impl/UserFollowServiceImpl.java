package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.entity.User;
import com.w.esshop.entity.UserFollow;
import com.w.esshop.mapper.UserFollowMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserFollowServiceImpl implements UserFollowService {

    @Autowired
    private UserFollowMapper followMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }
        // 已关注则直接返回，避免重复插入（唯一索引也会拦，但提前判断体验更好）
        if (isFollowing(followerId, followingId)) {
            return;
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreateTime(LocalDateTime.now());
        followMapper.insert(follow);
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            return false;
        }
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId)) > 0;
    }

    @Override
    public long followerCount(Long userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, userId));
    }

    @Override
    public long followingCount(Long userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId));
    }

    @Override
    public PageResult<User> followers(Long userId, Long pageNum, Long size) {
        Page<UserFollow> page = new Page<>(pageNum, size);
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        // 粉丝列表：要展示的是「关注我的人」，即 followerId 对应的用户
        return toUserPage(result, true);
    }

    @Override
    public PageResult<User> followings(Long userId, Long pageNum, Long size) {
        Page<UserFollow> page = new Page<>(pageNum, size);
        Page<UserFollow> result = followMapper.selectPage(page,
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        // 关注列表：要展示的是「我关注的人」，即 followingId 对应的用户
        return toUserPage(result, false);
    }

    /**
     * 把关注记录翻页结果转换成一页用户
     *
     * @param pickFollower true 取 followerId 对应的用户（粉丝列表）；
     *                     false 取 followingId 对应的用户（关注列表）
     */
    private PageResult<User> toUserPage(Page<UserFollow> result, boolean pickFollower) {
        List<UserFollow> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(),
                    Collections.emptyList());
        }

        List<Long> userIds = records.stream()
                .map(pickFollower ? UserFollow::getFollowerId : UserFollow::getFollowingId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<User> users = records.stream()
                .map(f -> userMap.get(pickFollower ? f.getFollowerId() : f.getFollowingId()))
                .filter(u -> u != null)
                .peek(u -> u.setPassword(null))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), users);
    }
}
