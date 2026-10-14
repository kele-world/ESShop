package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.common.PageResult;
import com.w.esshop.dto.ProductReviewVO;
import com.w.esshop.dto.UserProfileVO;
import com.w.esshop.entity.CommunityPost;
import com.w.esshop.entity.Product;
import com.w.esshop.entity.User;
import com.w.esshop.mapper.CommunityPostMapper;
import com.w.esshop.mapper.ProductMapper;
import com.w.esshop.mapper.UserMapper;
import com.w.esshop.service.CommunityPostService;
import com.w.esshop.service.ProductReviewService;
import com.w.esshop.service.ProductService;
import com.w.esshop.service.UserFollowService;
import com.w.esshop.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户主页实现
 *
 * 主页聚合了三块内容：发布的商品、发布的帖子、收到的评价。
 * 复用各模块已有的分页方法，避免重复实现查询逻辑。
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CommunityPostMapper postMapper;
    @Autowired
    private UserFollowService followService;
    @Autowired
    private ProductReviewService reviewService;
    @Autowired
    private ProductService productService;
    @Autowired
    private CommunityPostService communityPostService;

    @Override
    public UserProfileVO getProfile(Long userId, Long currentUserId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());

        vo.setAvgRating(reviewService.avgRating(userId));
        vo.setReviewCount(reviewService.reviewCount(userId));
        vo.setFollowerCount(followService.followerCount(userId));
        vo.setFollowingCount(followService.followingCount(userId));

        // 已发布的商品（只算在售的）
        vo.setProductCount(productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, userId)
                .eq(Product::getStatus, "ON_SALE")));

        // 已发布的帖子（只算审核通过的）
        vo.setPostCount(postMapper.selectCount(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getUserId, userId)
                .eq(CommunityPost::getStatus, "APPROVED")));

        // 是否是自己 / 是否已关注
        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        vo.setSelf(isSelf);
        vo.setFollowing(isSelf ? false : followService.isFollowing(currentUserId, userId));

        return vo;
    }

    @Override
    public PageResult<Product> products(Long userId, Long page, Long size) {
        // 复用商品服务：按用户筛选，且只在售、未删除
        com.w.esshop.dto.ProductQueryDTO query = new com.w.esshop.dto.ProductQueryDTO();
        query.setUserId(userId);
        query.setStatus("ON_SALE");
        query.setPage(page);
        query.setSize(size);
        return productService.page(query);
    }

    @Override
    public PageResult<CommunityPost> posts(Long userId, Long page, Long size) {
        // 复用社区服务：传当前用户为 null，不查询点赞/收藏状态
        return communityPostService.pageOfUser(userId, page, size);
    }

    @Override
    public ProductReviewVO reviews(Long userId, Long page, Long size) {
        return reviewService.listBySeller(userId, page, size);
    }
}
