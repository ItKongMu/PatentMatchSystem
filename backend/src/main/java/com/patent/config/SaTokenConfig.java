package com.patent.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token配置类
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定需要登录认证的路由
            SaRouter.match("/**")
                    // 排除不需要认证的路由
                    .notMatch(
                            "/api/auth/login",
                            "/api/auth/register",
                            "/api/patent/list",
                            "/api/patent/{id}",
                            "/api/patent/{id}/entities",
                            "/api/patent/{id}/domains",
                            "/api/patent/{id}/vector",
                            "/api/search",
                            "/api/search/advanced",
                            "/doc.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/webjars/**",
                            "/favicon.ico"
                    )
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
