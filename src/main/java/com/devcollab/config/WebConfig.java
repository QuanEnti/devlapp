package com.devcollab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler) throws Exception {

                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");

                return true;
            }
        })
                .addPathPatterns(
                        "/view/signin",
                        "/view/login", 
                        "/view/register",
                        "/view/verify-otp",
                        "/view/reset-password",
                        "/view/forgot-password",
                        "/view/password-reset-success")
                .order(1);
    }
}
