package com.w.esshop.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 客服对话记录
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    /** USER-用户, ASSISTANT-客服 */
    private String role;

    private String content;

    /** 命中的知识库ID */
    private Long matchedId;

    private LocalDateTime createTime;
}