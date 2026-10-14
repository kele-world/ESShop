package com.w.esshop.service;

import com.w.esshop.dto.ChatReplyVO;
import com.w.esshop.entity.ChatMessage;
import com.w.esshop.entity.ChatKnowledge;

import java.util.List;

/**
 * 智能客服服务
 */
public interface ChatService {

    /** 发送消息并获取回复 */
    ChatReplyVO chat(Long userId, String sessionId, String content);

    /** 获取会话历史 */
    List<ChatMessage> history(String sessionId);

    /** 清空会话 */
    void clear(String sessionId);

    /** 常见问题列表（引导用户） */
    List<ChatKnowledge> hotQuestions();
}