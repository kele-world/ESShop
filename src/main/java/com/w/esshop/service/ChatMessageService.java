package com.w.esshop.service;

import com.w.esshop.entity.Conversation;
import com.w.esshop.entity.PrivateMessage;

import java.util.List;

/**
 * 私聊服务
 *
 * 同时服务于 REST 接口和 WebSocket 消息处理。
 */
public interface ChatMessageService {

    /**
     * 获取或创建会话
     *
     * @param userId    发起人（通常是买家）
     * @param sellerId  卖家
     * @param productId 关联商品，可为 null
     */
    Conversation getOrCreateConversation(Long userId, Long sellerId, Long productId);

    /** 我的会话列表（带对方信息、未读数） */
    List<Conversation> myConversations(Long userId);

    /** 会话详情（校验归属） */
    Conversation getConversation(Long userId, Long conversationId);

    /** 会话内历史消息（按时间正序） */
    List<PrivateMessage> history(Long userId, Long conversationId, Long beforeId, int size);

    /**
     * 发送消息（持久化 + 更新会话摘要和未读数）
     *
     * @return 保存后的消息实体
     */
    PrivateMessage sendMessage(Long senderId, Long conversationId, String content);

    /** 标记会话已读（清零当前用户的未读数） */
    void markRead(Long userId, Long conversationId);

    /**
     * 会话还没有任何消息时，发送一条开场消息
     *
     * 用于「联系卖家」时自动带上商品信息，让卖家知道咨询的是哪件商品。
     * 已有对话则不发送，避免每次点击都重复发一条。
     */
    void sendOpeningMessageIfEmpty(Long senderId, Long conversationId, String content);

    /**
     * 删除单条消息（仅对自己隐藏）
     *
     * 不是真删除：数据仍保留在数据库中，对方也依然能看到。
     * 只有消息的发送方或接收方才能删除。
     */
    void deleteMessage(Long userId, Long messageId);

    /**
     * 清空整个会话（仅对自己隐藏）
     *
     * 把该会话里「所有我参与的消息」标记为已删除，
     * 效果等同于「我不再看到这段聊天记录」，对方不受影响。
     */
    void clearConversation(Long userId, Long conversationId);

    /**
     * 从会话列表中移除某个会话（仅对自己生效）
     *
     * 效果：
     *   - 会话不再出现在我的列表里，历史消息也对我隐藏
     *   - 对方列表不受影响
     *   - 对方之后发来新消息时，会话会重新出现在我的列表中
     */
    void removeConversation(Long userId, Long conversationId);

    /** 我的总未读数 */
    int totalUnread(Long userId);

    /** 判断用户是否属于该会话 */
    boolean isMember(Long userId, Long conversationId);
}
