package com.w.esshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客服对话请求
 */
@Data
public class ChatRequestDTO {

    /** 会话ID，前端生成并保持（用于串联多轮对话） */
    private String sessionId;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}