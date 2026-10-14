package com.w.esshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发布帖子请求
 */
@Data
public class CommunityPostDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    private String images;

    /** 分类ID */
    private Long categoryId;
}