package com.tiantian.yuaiagent.config;

import com.tiantian.yuaiagent.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器：AI 接口需要登录
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 路径不包含 context-path（/api），Spring 自动处理
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/ai/**");
    }
}
