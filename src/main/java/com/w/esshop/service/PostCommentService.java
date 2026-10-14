package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.entity.PostComment;
import java.util.List;

public interface PostCommentService {
    List<PostComment> listByPostId(Long postId);
    void add(Long userId, Long postId, String content);
    void delete(Long userId, Long commentId);
}