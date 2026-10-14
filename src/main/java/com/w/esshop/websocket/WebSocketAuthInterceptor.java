package com.w.esshop.websocket;

import com.w.esshop.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权
 *
 * 浏览器原生的 WebSocket API 不支持自定义请求头，
 * 所以 Token 通过 URL 参数传递：ws://host/ws/chat?token=xxx
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        String token = extractToken(query);

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手失败：缺少 token");
            return false;
        }
        if (!jwtUtil.isValid(token)) {
            log.warn("WebSocket 握手失败：token 无效或已过期");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            log.warn("WebSocket 握手失败：无法解析用户ID");
            return false;
        }

        // 把用户信息放进 session 属性，后续 handler 直接取用
        attributes.put("userId", userId);
        attributes.put("username", jwtUtil.getUsername(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    /** 从查询串中解析 token 参数 */
    private String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                return pair.substring(idx + 1);
            }
        }
        return null;
    }
}
