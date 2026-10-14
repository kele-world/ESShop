package com.w.esshop.interceptor;

import com.w.esshop.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 软鉴权拦截器 —— 用于「游客可访问，但登录用户能看到个性化状态」的接口
 *
 * 与 JwtInterceptor 的区别：
 *   - JwtInterceptor：没 Token 直接 401（强制登录）
 *   - 本拦截器：没 Token 也放行，有 Token 则解析出用户信息
 *
 * 典型场景：商品详情页
 *   - 游客能看商品（不登录也能逛）
 *   - 登录用户能额外看到「是否已收藏」的状态
 *
 * 注意：本拦截器不做 Redis 校验，因为它的职责只是「尽力识别用户」，
 * 识别失败不影响业务，真正的强校验仍由 JwtInterceptor 负责。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 没有 Token 或 Token 无效都放行，只是不设置用户信息（按游客处理）
        if (token == null || token.isEmpty() || !jwtUtil.isValid(token)) {
            return true;
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
                request.setAttribute("username", jwtUtil.getUsername(token));
                request.setAttribute("role", jwtUtil.getRole(token));
            }
        } catch (Exception e) {
            log.debug("软鉴权解析 Token 失败，按游客处理: {}", e.getMessage());
        }

        return true;
    }
}
