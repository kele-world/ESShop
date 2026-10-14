package com.w.esshop.controller;

import com.w.esshop.common.Result;
import com.w.esshop.entity.Conversation;
import com.w.esshop.entity.PrivateMessage;
import com.w.esshop.service.ChatMessageService;
import com.w.esshop.websocket.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 私聊控制器（REST 部分）
 *
 * 消息收发走 WebSocket（/ws/chat），这里只负责会话管理、历史记录、
 * 已读标记等不适合走 WebSocket 的操作。
 */
@Tag(name = "私聊模块", description = "买卖双方聊天会话管理")
@RestController
@RequestMapping("/api/chat-message")
public class ChatMessageController {

    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private WebSocketSessionManager sessionManager;

    @Operation(summary = "获取或创建会话")
    @PostMapping("/conversation")
    public Result<Conversation> createConversation(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long sellerId = Long.valueOf(String.valueOf(body.get("sellerId")));
        Long productId = body.get("productId") == null ? null
                : Long.valueOf(String.valueOf(body.get("productId")));

        Conversation conv = chatMessageService.getOrCreateConversation(userId, sellerId, productId);

        // 可选：会话为空时自动发一条开场消息
        // 从商品页「联系卖家」进入时会带上商品信息，让卖家知道咨询的是哪件商品
        Object opening = body.get("openingMessage");
        if (opening != null && !String.valueOf(opening).isBlank()) {
            chatMessageService.sendOpeningMessageIfEmpty(
                    userId, conv.getId(), String.valueOf(opening));
        }

        return Result.success(conv);
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("/conversations")
    public Result<List<Conversation>> conversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatMessageService.myConversations(userId));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversation/{id}")
    public Result<Conversation> conversation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatMessageService.getConversation(userId, id));
    }

    @Operation(summary = "历史消息")
    @GetMapping("/conversation/{id}/messages")
    public Result<List<PrivateMessage>> messages(@PathVariable Long id,
                                                  @RequestParam(required = false) Long beforeId,
                                                  @RequestParam(defaultValue = "30") Integer size,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatMessageService.history(userId, id, beforeId, size));
    }

    @Operation(summary = "标记会话已读")
    @PutMapping("/conversation/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.markRead(userId, id);
        return Result.success("已读");
    }

    @Operation(summary = "删除单条消息（仅自己不可见）")
    @DeleteMapping("/message/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.deleteMessage(userId, messageId);
        return Result.success("已删除");
    }

    @Operation(summary = "清空会话记录（仅自己不可见）")
    @DeleteMapping("/conversation/{id}/messages")
    public Result<Void> clearConversation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.clearConversation(userId, id);
        return Result.success("已清空");
    }

    @Operation(summary = "从列表中移除会话（仅自己不可见，对方发新消息会重新出现）")
    @DeleteMapping("/conversation/{id}")
    public Result<Void> removeConversation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageService.removeConversation(userId, id);
        return Result.success("已移除");
    }

    @Operation(summary = "我的未读消息总数")
    @GetMapping("/unread")
    public Result<Integer> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatMessageService.totalUnread(userId));
    }

    @Operation(summary = "查询用户在线状态")
    @PostMapping("/online-status")
    public Result<Map<Long, Boolean>> onlineStatus(@RequestBody Map<String, Object> body) {
        Object ids = body.get("userIds");
        Map<Long, Boolean> result = new HashMap<>();
        if (ids instanceof List<?> list) {
            for (Object id : list) {
                Long uid = Long.valueOf(String.valueOf(id));
                result.put(uid, sessionManager.isOnline(uid));
            }
        }
        return Result.success(result);
    }
}
