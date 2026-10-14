package com.w.esshop.service;

import com.w.esshop.common.PageResult;
import com.w.esshop.entity.UserFavorite;

public interface ProductFavoriteService {
    void favorite(Long userId, Long productId);
    void unfavorite(Long userId, Long productId);
    boolean isFavorited(Long userId, Long productId);
    PageResult<UserFavorite> myFavorites(Long userId, Long page, Long size);
}