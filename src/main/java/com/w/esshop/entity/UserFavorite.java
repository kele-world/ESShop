package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_favorite")
public class UserFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long targetId;
    private String type;  // POST, PRODUCT
    private LocalDateTime createTime;

    // ---- 非数据库字段 ----
    @TableField(exist = false)
    private String targetTitle;
    @TableField(exist = false)
    private String targetImage;
}