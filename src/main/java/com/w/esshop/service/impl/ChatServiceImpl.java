package com.w.esshop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.w.esshop.client.AiChatClient;
import com.w.esshop.dto.ChatReplyVO;
import com.w.esshop.entity.ChatKnowledge;
import com.w.esshop.entity.ChatMessage;
import com.w.esshop.mapper.ChatKnowledgeMapper;
import com.w.esshop.mapper.ChatMessageMapper;
import com.w.esshop.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能客服实现
 *
 * 双引擎策略：
 *   1. 优先调用 Python 智能客服服务（LangChain + 大模型）
 *   2. 服务不可用时自动降级到关键词规则库
 */
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatKnowledgeMapper knowledgeMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private AiChatClient aiChatClient;

    /** 命中阈值：得分低于此值认为未命中 */
    private static final int MATCH_THRESHOLD = 2;

    private static final String FALLBACK_ANSWER =
            "抱歉，我暂时没有找到相关的答案。\n\n" +
            "你可以换个说法再问一次，或者试试下面的常见问题：\n" +
            "• 怎么购买商品？\n" +
            "• 怎么发布商品？\n" +
            "• 怎么取消订单？\n" +
            "• 交易要注意什么？\n\n" +
            "如果问题仍未解决，建议联系平台管理员。";

    @Override
    public ChatReplyVO chat(Long userId, String sessionId, String content) {
        // 记录用户消息
        saveMessage(sessionId, userId, "USER", content, null);

        // 1. 优先走智能客服服务
        String aiReply = aiChatClient.chat(sessionId, userId, content);
        if (aiReply != null && !aiReply.isEmpty()) {
            ChatReplyVO aiVo = new ChatReplyVO();
            aiVo.setContent(aiReply);
            aiVo.setMatched(true);
            aiVo.setSource("AI");
            aiVo.setSuggestions(List.of());
            saveMessage(sessionId, userId, "ASSISTANT", aiReply, null);
            return aiVo;
        }

        // 2. 客服服务不可用，降级到关键词规则库
        ChatKnowledge matched = match(content);

        ChatReplyVO reply = new ChatReplyVO();
        reply.setSource("RULE");
        if (matched != null) {
            reply.setContent(matched.getAnswer());
            reply.setMatched(true);
            reply.setCategory(matched.getCategory());
            reply.setSuggestions(List.of());
        } else {
            reply.setContent(FALLBACK_ANSWER);
            reply.setMatched(false);
            reply.setSuggestions(hotQuestionTitles());
        }

        saveMessage(sessionId, userId, "ASSISTANT", reply.getContent(),
                matched != null ? matched.getId() : null);

        return reply;
    }

    @Override
    public List<ChatMessage> history(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        wrapper.orderByAsc(ChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    @Override
    public void clear(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        messageMapper.delete(wrapper);
        // 同时清空 Python 侧的多轮对话记忆
        aiChatClient.clearSession(sessionId);
    }

    @Override
    public List<ChatKnowledge> hotQuestions() {
        LambdaQueryWrapper<ChatKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatKnowledge::getEnabled, 1);
        wrapper.orderByAsc(ChatKnowledge::getSort);
        wrapper.last("LIMIT 6");
        return knowledgeMapper.selectList(wrapper);
    }

    // ==================== 匹配算法 ====================

    /**
     * 关键词打分匹配
     *
     * 打分规则：
     * - 关键词命中：分数 = 关键词长度（越具体的关键词权重越高）
     * - 问题标题完全包含在用户输入中：额外加 5 分
     * - 所有命中分数累加，取最高分
     */
    private ChatKnowledge match(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        String input = userInput.trim().toLowerCase();

        LambdaQueryWrapper<ChatKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatKnowledge::getEnabled, 1);
        List<ChatKnowledge> all = knowledgeMapper.selectList(wrapper);

        ChatKnowledge best = null;
        int bestScore = 0;

        for (ChatKnowledge kb : all) {
            int score = score(input, kb);
            if (score > bestScore) {
                bestScore = score;
                best = kb;
            }
        }

        return bestScore >= MATCH_THRESHOLD ? best : null;
    }

    /**
     * 计算单条知识库的匹配得分
     */
    private int score(String input, ChatKnowledge kb) {
        int score = 0;

        // 1. 关键词命中累加，按关键词长度加权
        if (kb.getKeywords() != null && !kb.getKeywords().isEmpty()) {
            for (String kw : kb.getKeywords().split(",")) {
                String keyword = kw.trim().toLowerCase();
                if (!keyword.isEmpty() && input.contains(keyword)) {
                    score += keyword.length();
                }
            }
        }

        // 2. 问题标题被完整包含，额外加分（强信号）
        if (kb.getQuestion() != null) {
            String question = kb.getQuestion().replace("？", "").replace("?", "").trim().toLowerCase();
            if (!question.isEmpty() && input.contains(question)) {
                score += 5;
            }
        }

        return score;
    }

    private List<String> hotQuestionTitles() {
        List<String> titles = new ArrayList<>();
        for (ChatKnowledge kb : hotQuestions()) {
            titles.add(kb.getQuestion());
        }
        return titles;
    }

    private void saveMessage(String sessionId, Long userId, String role, String content, Long matchedId) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMatchedId(matchedId);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }
}