package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.w.esshop.common.PageResult;
import com.w.esshop.dto.CommunityPostDTO;
import com.w.esshop.entity.*;
import com.w.esshop.mapper.*;
import com.w.esshop.service.CommunityPostService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommunityPostServiceImpl implements CommunityPostService {

    @Autowired private CommunityPostMapper postMapper;
    @Autowired private CommunityCategoryMapper categoryMapper;
    @Autowired private PostLikeMapper likeMapper;
    @Autowired private UserFavoriteMapper favoriteMapper;
    @Autowired private PostCommentMapper commentMapper;
    @Autowired private UserMapper userMapper;

    @Override
    public void publish(Long userId, CommunityPostDTO dto) {
        CommunityPost post = new CommunityPost();
        BeanUtils.copyProperties(dto, post);
        post.setUserId(userId);
        post.setStatus("PENDING");
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
    }

    @Override
    public PageResult<CommunityPost> page(Long userId, Long pageNum, Long size, Long categoryId, String keyword) {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, "APPROVED");
        if (categoryId != null) wrapper.eq(CommunityPost::getCategoryId, categoryId);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(CommunityPost::getTitle, keyword);
        wrapper.orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> page = new Page<>(pageNum, size);
        Page<CommunityPost> result = postMapper.selectPage(page, wrapper);
        fillExtraInfo(result.getRecords(), userId);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResult<CommunityPost> pageOfUser(Long userId, Long pageNum, Long size) {
        Page<CommunityPost> page = new Page<>(pageNum, size);
        Page<CommunityPost> result = postMapper.selectPage(page,
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getUserId, userId)
                        .eq(CommunityPost::getStatus, "APPROVED")
                        .orderByDesc(CommunityPost::getCreateTime));
        // 传 null 表示不查当前用户的点赞/收藏状态（浏览他人主页时不需要）
        fillExtraInfo(result.getRecords(), null);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public CommunityPost detail(Long userId, Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) throw new RuntimeException("帖子不存在");
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        fillExtraInfo(List.of(post), userId);
        return post;
    }

    @Override
    public void delete(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权删除");
        postMapper.deleteById(postId);
    }

    // ==================== 点赞 ====================

    @Override
    @Transactional
    public void like(Long userId, Long postId) {
        if (likeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId)) > 0) return;
        PostLike like = new PostLike();
        like.setPostId(postId); like.setUserId(userId); like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) { post.setLikeCount(post.getLikeCount() + 1); postMapper.updateById(post); }
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long postId) {
        likeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        CommunityPost post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) { post.setLikeCount(post.getLikeCount() - 1); postMapper.updateById(post); }
    }

    // ==================== 收藏 ====================

    @Override
    public void favorite(Long userId, Long postId) {
        if (favoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId).eq(UserFavorite::getTargetId, postId).eq(UserFavorite::getType, "POST")) > 0) return;
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId); fav.setTargetId(postId); fav.setType("POST"); fav.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(fav);
        // 收藏数 +1
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            int count = post.getFavoriteCount() == null ? 0 : post.getFavoriteCount();
            post.setFavoriteCount(count + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    public void unfavorite(Long userId, Long postId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId).eq(UserFavorite::getTargetId, postId).eq(UserFavorite::getType, "POST"));
        // 只有确实删掉了记录才减计数
        if (deleted > 0) {
            CommunityPost post = postMapper.selectById(postId);
            if (post != null && post.getFavoriteCount() != null && post.getFavoriteCount() > 0) {
                post.setFavoriteCount(post.getFavoriteCount() - 1);
                postMapper.updateById(post);
            }
        }
    }

    @Override
    public PageResult<UserFavorite> myFavorites(Long userId, Long pageNum, Long size) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId).eq(UserFavorite::getType, "POST");
        wrapper.orderByDesc(UserFavorite::getCreateTime);
        Page<UserFavorite> page = new Page<>(pageNum, size);
        Page<UserFavorite> result = favoriteMapper.selectPage(page, wrapper);
        if (!result.getRecords().isEmpty()) {
            List<Long> postIds = result.getRecords().stream().map(UserFavorite::getTargetId).toList();
            Map<Long, CommunityPost> postMap = postMapper.selectBatchIds(postIds).stream()
                    .collect(Collectors.toMap(CommunityPost::getId, p -> p));
            result.getRecords().forEach(f -> {
                CommunityPost p = postMap.get(f.getTargetId());
                if (p != null) { f.setTargetTitle(p.getTitle()); f.setTargetImage(p.getImages()); }
            });
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    // ==================== 分类 ====================

    @Override
    public List<CommunityCategory> categoryList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CommunityCategory>().orderByAsc(CommunityCategory::getSort));
    }

    // ==================== 管理员 ====================

    @Override public PageResult<CommunityPost> adminPendingPage(Long pageNum, Long size) {
        Page<CommunityPost> page = new Page<>(pageNum, size);
        Page<CommunityPost> result = postMapper.selectPage(page,
                new LambdaQueryWrapper<CommunityPost>().eq(CommunityPost::getStatus, "PENDING").orderByDesc(CommunityPost::getCreateTime));
        fillExtraInfo(result.getRecords(), null);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override public PageResult<CommunityPost> adminAllPage(Long pageNum, Long size) {
        Page<CommunityPost> page = new Page<>(pageNum, size);
        Page<CommunityPost> result = postMapper.selectPage(page,
                new LambdaQueryWrapper<CommunityPost>().ne(CommunityPost::getStatus, "OFF_SHELF").orderByDesc(CommunityPost::getCreateTime));
        fillExtraInfo(result.getRecords(), null);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override public PageResult<CommunityPost> adminOffShelfPage(Long pageNum, Long size) {
        Page<CommunityPost> page = new Page<>(pageNum, size);
        Page<CommunityPost> result = postMapper.selectPage(page,
                new LambdaQueryWrapper<CommunityPost>().eq(CommunityPost::getStatus, "OFF_SHELF").orderByDesc(CommunityPost::getCreateTime));
        fillExtraInfo(result.getRecords(), null);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override public void approve(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        post.setStatus("APPROVED"); post.setUpdateTime(LocalDateTime.now()); postMapper.updateById(post);
    }

    @Override public void reject(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        post.setStatus("REJECTED"); post.setUpdateTime(LocalDateTime.now()); postMapper.updateById(post);
    }

    @Override public void adminOffShelf(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        post.setStatus("OFF_SHELF"); post.setUpdateTime(LocalDateTime.now()); postMapper.updateById(post);
    }

    @Override public void adminReList(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new RuntimeException("帖子不存在");
        post.setStatus("APPROVED"); post.setUpdateTime(LocalDateTime.now()); postMapper.updateById(post);
    }

    @Override public void adminDelete(Long postId) {
        postMapper.deleteById(postId);
    }

    // ==================== 辅助方法 ====================

    private void fillExtraInfo(List<CommunityPost> posts, Long currentUserId) {
        if (posts.isEmpty()) return;
        List<Long> userIds = posts.stream().map(CommunityPost::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        // 分类名
        List<Long> catIds = posts.stream().map(CommunityPost::getCategoryId).filter(id -> id != null).distinct().toList();
        Map<Long, String> catMap = new java.util.HashMap<>();
        if (!catIds.isEmpty() && categoryMapper != null) {
            List<CommunityCategory> cats = categoryMapper.selectBatchIds(catIds);
            if (cats != null) {
                cats.forEach(c -> catMap.put(c.getId(), c.getName()));
            }
        }
        // 当前用户点赞/收藏状态
        Set<Long> likedIds = new java.util.HashSet<>(), favoritedIds = new java.util.HashSet<>();
        if (currentUserId != null) {
            List<Long> postIds = posts.stream().map(CommunityPost::getId).toList();
            List<PostLike> likes = likeMapper.selectList(new LambdaQueryWrapper<PostLike>().eq(PostLike::getUserId, currentUserId).in(PostLike::getPostId, postIds));
            if (likes != null) likedIds = likes.stream().map(PostLike::getPostId).collect(Collectors.toSet());
            List<UserFavorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, currentUserId)
                    .eq(UserFavorite::getType, "POST").in(UserFavorite::getTargetId, postIds));
            if (favs != null) favoritedIds = favs.stream().map(UserFavorite::getTargetId).collect(Collectors.toSet());
        }
        for (CommunityPost p : posts) {
            User u = userMap.get(p.getUserId());
            if (u != null) { p.setAuthorName(u.getNickname()); p.setAuthorAvatar(u.getAvatar()); }
            p.setCategoryName(catMap.getOrDefault(p.getCategoryId(), ""));
            p.setLiked(likedIds.contains(p.getId()));
            p.setFavorited(favoritedIds.contains(p.getId()));
        }
    }
}