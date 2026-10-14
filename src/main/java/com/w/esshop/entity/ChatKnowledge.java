package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 智能客服知识库
 */
@Data
@TableName("chat_knowledge")
public class ChatKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    private String question;

    /** 关键词，逗号分隔 */
    private String keywords;

    private String answer;

    private Integer sort;

    /** 0-停用, 1-启用 */
    private Integer enabled;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}