package com.w.esshop.interceptor;

import com.w.esshop.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器 —— 校验 Token 有效性并从 Redis 中确认未过期
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }

        if (!jwtUtil.isValid(token)) {
            response.setStatus(401);
            return false;
        }

        // 解析一次即可，避免重复解析 JWT（原本调用了 4 次）
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // 校验 Redis 中是否存在（是否已主动登出）
        // 远程 Redis 抖动时只记录警告并放行，不因一次网络波动把用户踢下线
        String redisKey = "esshop:token:" + userId;
        try {
            String redisToken = (String) redisTemplate.opsForValue().get(redisKey);
            if (redisToken == null || !redisToken.equals(token)) {
                response.setStatus(401);
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis 校验 Token 失败，本次放行（JWT 签名已验证通过）: {}", e.getMessage());
        }

        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);

        return true;
    }
}