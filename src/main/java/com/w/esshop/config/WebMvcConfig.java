package com.w.esshop.config;

import com.w.esshop.interceptor.JwtInterceptor;
import com.w.esshop.interceptor.OptionalAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置 —— 注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private OptionalAuthInterceptor optionalAuthInterceptor;

    /**
     * 游客可访问的接口（不强制登录）
     *
     * 这些接口走软鉴权：游客能看，登录用户能看到个性化状态
     * （比如商品详情的「是否已收藏」、用户主页的「是否已关注」）
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/user/register",
            "/api/user/login",
            "/api/category/list",
            // 首页运营位：游客也要能看到
            "/api/banner/list",
            "/api/product/list",
            "/api/product/detail/**",
            "/api/chat/send",
            "/api/chat/hot-questions",
            // 用户主页：游客也能点进别人的主页看资料、商品和帖子、评价。
            // 注意只开放只读接口，关注/粉丝列表等需要登录，不在此列。
            "/api/user-profile/*",
            "/api/user-profile/*/products",
            "/api/user-profile/*/posts",
            "/api/user-profile/*/reviews"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 强制登录拦截器：除公开接口外，其余 /api/** 必须带有效 Token
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(PUBLIC_ENDPOINTS)
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html"
                )
                .order(1);

        // 2. 软鉴权拦截器：只作用于公开接口。
        //    游客照常访问；已登录用户则解析出 userId，
        //    让「商品详情」能返回「是否已收藏」这类个性化状态。
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns(PUBLIC_ENDPOINTS)
                .order(2);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 使用绝对路径，避免相对路径在不同启动目录下找不到文件
        String absolutePath = new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);
    }
}