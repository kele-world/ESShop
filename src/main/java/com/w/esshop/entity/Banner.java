package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页运营位（轮播图）
 */
@Data
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String subtitle;

    /** 背景图 URL，为空则前端用渐变兜底 */
    private String image;

    /** 点击跳转地址 */
    private String link;

    /** 按钮文字，为空则不显示按钮 */
    private String buttonText;

    /** ACTIVITY-活动, ANNOUNCEMENT-公告, GUIDE-指引 */
    private String type;

    private Integer sort;

    /** 0-停用, 1-启用 */
    private Integer enabled;

    /** 生效开始时间，为空表示立即生效 */
    private LocalDateTime startTime;

    /** 生效结束时间，为空表示永久有效 */
    private LocalDateTime endTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
