package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.PostComment;
import com.w.esshop.entity.User;
import com.w.esshop.mapper.CommunityPostMapper;
import com.w.esshop.mapper.PostCommentMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.PostCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostCommentServiceImpl implements PostCommentService {

    @Autowired
    private PostCommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CommunityPostMapper postMapper;

    @Override
    public List<PostComment> listByPostId(Long postId) {
        LambdaQueryWrapper<PostComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostComment::getPostId, postId);
        wrapper.orderByAsc(PostComment::getCreateTime);
        List<PostComment> comments = commentMapper.selectList(wrapper);
        if (!comments.isEmpty()) {
            List<Long> userIds = comments.stream().map(PostComment::getUserId).distinct().toList();
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            comments.forEach(c -> {
                User u = userMap.get(c.getUserId());
                if (u != null) { c.setAuthorName(u.getNickname()); c.setAuthorAvatar(u.getAvatar()); }
            });
        }
        return comments;
    }

    @Override
    @Transactional
    public void add(Long userId, Long postId, String content) {
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        // 评论数+1
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long commentId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new RuntimeException("评论不存在");
        if (!comment.getUserId().equals(userId)) throw new RuntimeException("无权删除他人评论");
        commentMapper.deleteById(commentId);
        // 评论数-1
        CommunityPost post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
    }
}