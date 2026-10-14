package com.w.esshop.websocket;

import com.w.esshop.entity.Conversation;
import com.w.esshop.entity.PrivateMessage;
import com.w.esshop.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天 WebSocket 处理器
 *
 * 客户端消息格式：
 *   {"type":"CHAT",   "conversationId":1, "content":"你好"}
 *   {"type":"READ",   "conversationId":1}
 *   {"type":"PING"}
 *
 * 服务端推送格式：
 *   {"type":"CHAT",    "data":{...消息体...}}
 *   {"type":"READ",    "data":{"conversationId":1, "readerId":2}}
 *   {"type":"PONG"}
 *   {"type":"ERROR",   "message":"..."}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ChatMessageService chatMessageService;

    /** 用 Spring 容器里的 ObjectMapper，它已注册 JSR-310 模块，能处理 LocalDateTime */
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        // 只有从「无连接」变成「有连接」才算刚上线，
        // 同一用户开第二个标签页不重复广播
        boolean wasOffline = !sessionManager.isOnline(userId);
        sessionManager.add(userId, session);
        if (wasOffline) {
            broadcastPresence(userId, true);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText("");

            switch (type) {
                case "CHAT" -> handleChat(userId, node);
                case "READ" -> handleRead(userId, node);
                case "PING" -> send(session, Map.of("type", "PONG"));
                default -> send(session, Map.of("type", "ERROR", "message", "未知的消息类型: " + type));
            }
        } catch (Exception e) {
            log.warn("处理 WebSocket 消息失败: {}", e.getMessage());
            send(session, Map.of("type", "ERROR", "message", e.getMessage() == null ? "消息处理失败" : e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            sessionManager.remove(userId, session);
            // 关掉最后一个连接才算真正下线
            if (!sessionManager.isOnline(userId)) {
                broadcastPresence(userId, false);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket 传输错误: {}", exception.getMessage());
        Long userId = getUserId(session);
        if (userId != null) {
            sessionManager.remove(userId, session);
            if (!sessionManager.isOnline(userId)) {
                broadcastPresence(userId, false);
            }
        }
    }

    // ==================== 消息处理 ====================

    /** 处理聊天消息：落库 → 回执给发送者 → 推送给接收者 */
    private void handleChat(Long userId, JsonNode node) {
        Long conversationId = node.path("conversationId").asLong();
        String content = node.path("content").asText("");

        PrivateMessage msg = chatMessageService.sendMessage(userId, conversationId, content);

        Conversation conv = chatMessageService.getConversation(userId, conversationId);
        Long receiverId = userId.equals(conv.getBuyerId()) ? conv.getSellerId() : conv.getBuyerId();

        Map<String, Object> chatPayload = new HashMap<>();
        chatPayload.put("type", "CHAT");
        chatPayload.put("data", msg);

        // 推给接收者（不在线也没关系，消息已落库，下次上线能看到）
        boolean delivered = sessionManager.sendToUser(receiverId, chatPayload);

        // 回执给发送者，让前端把"发送中"变成"已送达/已发送"
        chatPayload.put("self", true);
        chatPayload.put("delivered", delivered);
        sessionManager.sendToUser(userId, chatPayload);
    }

    /** 处理已读回执：告诉对方"你的消息我读了" */
    private void handleRead(Long userId, JsonNode node) {
        Long conversationId = node.path("conversationId").asLong();
        chatMessageService.markRead(userId, conversationId);

        Conversation conv = chatMessageService.getConversation(userId, conversationId);
        Long peerId = userId.equals(conv.getBuyerId()) ? conv.getSellerId() : conv.getBuyerId();

        sessionManager.sendToUser(peerId, Map.of(
                "type", "READ",
                "data", Map.of("conversationId", conversationId, "readerId", userId)
        ));
    }

    /**
     * 广播某人的上下线状态
     *
     * 只推给「和他有会话的人」，不打扰无关用户。
     * 前端据此在聊天窗口显示对方的在线状态。
     */
    private void broadcastPresence(Long userId, boolean online) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", "PRESENCE",
                    "data", Map.of("userId", userId, "online", online)
            );
            chatMessageService.myConversations(userId).forEach(conv -> {
                Long peerId = userId.equals(conv.getBuyerId()) ? conv.getSellerId() : conv.getBuyerId();
                sessionManager.sendToUser(peerId, payload);
            });
        } catch (Exception e) {
            // 在线状态只是锦上添花，失败不影响聊天主流程
            log.debug("广播在线状态失败: {}", e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId == null ? null : (Long) userId;
    }

    private void send(WebSocketSession session, Object payload) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.warn("发送 WebSocket 消息失败: {}", e.getMessage());
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception ignored) {
        }
    }
}
