package com.w.esshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 配置
 *
 * 项目使用 Spring Boot 4.x，内置的是 Jackson 3（包名 tools.jackson）。
 *
 * 与 Jackson 2 的区别：
 *   - 时间类型（LocalDateTime 等）支持已内置在 jackson-databind 中，
 *     不需要额外引入 jackson-datatype-jsr310，也没有 tools.jackson.datatype.jsr310 这个包
 *   - 序列化异常是运行时异常，不需要 try-catch 受检异常
 *
 * 这里显式提供一个 ObjectMapper Bean，供 WebSocket 消息推送使用（容器中默认没有）。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
}
