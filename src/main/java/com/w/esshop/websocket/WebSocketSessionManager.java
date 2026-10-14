package com.w.esshop.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 在线会话管理
 *
 * 一个用户可能开多个标签页，所以 userId → 多个 session。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    /** userId -> 该用户的全部连接 */
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 使用 Spring 容器管理的 ObjectMapper。
     * 不能自己 new：裸 ObjectMapper 不认识 LocalDateTime，
     * 序列化消息时会抛 InvalidDefinitionException。
     */
    private final ObjectMapper objectMapper;

    public void add(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("用户 {} 上线，当前连接数 {}，在线用户数 {}", userId,
                userSessions.get(userId).size(), userSessions.size());
    }

    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
        log.debug("用户 {} 断开一个连接，剩余在线用户数 {}", userId, userSessions.size());
    }

    /** 用户是否在线 */
    public boolean isOnline(Long userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 给指定用户推送消息
     *
     * @return 是否成功送达（用户不在线返回 false）
     */
    public boolean sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("序列化 WebSocket 消息失败", e);
            return false;
        }

        boolean sent = false;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    // WebSocketSession 不是线程安全的，并发写会抛 IllegalStateException
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                    sent = true;
                } catch (Exception e) {
                    log.warn("推送消息给用户 {} 失败: {}", userId, e.getMessage());
                }
            }
        }
        return sent;
    }

    /** 当前在线用户数，用于监控 */
    public int onlineCount() {
        return userSessions.size();
    }
}
