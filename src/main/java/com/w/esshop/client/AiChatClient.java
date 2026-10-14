package com.w.esshop.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Python 智能客服服务客户端
 *
 * 调用不通或返回降级标记时返回 null，由调用方降级到规则库。
 */
@Slf4j
@Component
public class AiChatClient {

    @Value("${ai.service.url:http://localhost:8000}")
    private String baseUrl;

    @Value("${ai.service.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public AiChatClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(35000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用智能客服服务
     *
     * @return 客服回复；服务不可用或降级时返回 null
     */
    public String chat(String sessionId, Long userId, String message) {
        if (!enabled) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "session_id", sessionId,
                    "user_id", userId == null ? 0 : userId,
                    "message", message
            );

            ResponseEntity<AiResponse> response = restTemplate.postForEntity(
                    baseUrl + "/chat",
                    new HttpEntity<>(body, headers),
                    AiResponse.class
            );

            AiResponse result = response.getBody();
            if (result == null || Boolean.TRUE.equals(result.getDegraded())) {
                log.warn("智能客服服务降级: {}", result != null ? result.getReason() : "响应为空");
                return null;
            }
            return result.getReply();

        } catch (Exception e) {
            log.warn("智能客服服务调用失败，降级到规则库: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清空会话记忆
     */
    public void clearSession(String sessionId) {
        if (!enabled) return;
        try {
            restTemplate.delete(baseUrl + "/session/" + sessionId);
        } catch (Exception e) {
            log.debug("清空客服会话失败: {}", e.getMessage());
        }
    }

    /**
     * 探测智能客服服务是否可用
     */
    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 商品语义搜索
     *
     * 用户用自然语言描述需求（如"有没有便宜的代步工具"），
     * Python 侧做向量检索 + 查询扩展，返回匹配的商品ID及相似度。
     *
     * @return 命中的商品ID列表（按相似度降序）；服务不可用时返回空列表
     */
    public List<Long> searchProducts(String query, int topK) {
        if (!enabled) {
            return Collections.emptyList();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("query", query, "top_k", topK);

            ResponseEntity<ProductSearchResponse> response = restTemplate.postForEntity(
                    baseUrl + "/search-products",
                    new HttpEntity<>(body, headers),
                    ProductSearchResponse.class
            );

            ProductSearchResponse result = response.getBody();
            if (result == null || Boolean.TRUE.equals(result.getDegraded())
                    || result.getResults() == null) {
                return Collections.emptyList();
            }
            return result.getResults().stream()
                    .map(ProductSearchResponse.Item::getProductId)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            // 语义搜索是增强功能，失败时前端回退到普通关键词搜索
            log.warn("商品语义搜索失败，回退到关键词搜索: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Data
    public static class AiResponse {
        private String reply;
        private Boolean degraded;
        private String reason;
    }

    @Data
    public static class ProductSearchResponse {
        private List<Item> results;
        private Boolean degraded;
        private String reason;

        @Data
        public static class Item {
            private Long productId;
            private String title;
            private java.math.BigDecimal price;
            private Double score;
        }
    }
}