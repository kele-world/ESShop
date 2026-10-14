package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 社区帖子实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("community_post")
public class CommunityPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    private String content;

    private String images;

    private String status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    /** 收藏数 */
    private Integer favoriteCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorAvatar;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private Boolean liked;     // 当前用户是否已点赞
    @TableField(exist = false)
    private Boolean favorited; // 当前用户是否已收藏
}