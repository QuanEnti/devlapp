package com.devcollab.config;

import com.devcollab.security.JwtAuthenticationFilter;
import com.devcollab.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        // 🔹 1️⃣ API Security (JWT)
        @Bean
        @Order(1)
        public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // ✅ Cho phép các API public (login, register, verify, etc.)
                                                .requestMatchers("/api/auth/**", "/api/users/**","/api/admin/**").permitAll()
                                                // 🔒 Các API khác yêu cầu JWT
                                                .anyRequest().authenticated())
                                // ✅ Đặt JWT filter sau khi cho phép /api/auth/**
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout.disable());

                return http.build();
        }

        // 🔹 2️⃣ View Security (Thymeleaf + OAuth2)
        @Bean
        @Order(2)
        public SecurityFilterChain viewSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/view/**", "/", "/login/**", "/oauth2/**")
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                // ✅ Cho phép các tài nguyên public
                                                .requestMatchers(
                                                                "/", "/view/**",
                                                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                                                "/webjars/**",
                                                                "/oauth2/**", "/login/**")
                                                .permitAll()
                                                // 🔒 Các trang còn lại yêu cầu login
                                                .anyRequest().authenticated())
                                // ✅ Cấu hình OAuth2 login
                                .oauth2Login(oauth -> oauth
                                                .loginPage("/view/signin")
                                                .authorizationEndpoint(
                                                                config -> config.baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(config -> config.baseUri("/login/oauth2/code/*"))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureUrl("/view/signin?error=oauth"))
                                // ✅ Cho phép form login truyền thống
                                .formLogin(form -> form
                                                .loginPage("/view/signin")
                                                .defaultSuccessUrl("/view/dashboard", true)
                                                .permitAll())
                                // ✅ Cấu hình logout
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/view/signin?logout")
                                                .permitAll());

                return http.build();
        }

      
}
