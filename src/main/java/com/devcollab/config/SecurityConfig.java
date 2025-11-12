package com.devcollab.config;

import com.devcollab.security.JwtAuthenticationFilter;
import com.devcollab.security.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        // 🧩 API security
        @Bean
        @Order(1)
        public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(
                                                sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                // Các API public
                                                .requestMatchers(
                                                                "/api/auth/**",
                                                                "/api/users/**",
                                                                "api/payment/webhook"
                                                                // "/api/pm/public/**",
                                                                )
                                                .permitAll()
                                                // Admin-only APIs
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                // 🔹 Cho phép truy cập dashboard public nếu có
                                                // .requestMatchers("/api/pm/project/*/dashboard").permitAll()
                                                // 🔹 API join cần đăng nhập (Bearer hoặc cookie JWT)
                                                .requestMatchers("/api/tasks/**").authenticated()
                                                .requestMatchers("/api/pm/invite/join/**").authenticated()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout.disable());

                return http.build();
        }

        // 🎨 View security
        @Bean
        @Order(2)
        public SecurityFilterChain viewSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/view/**", "/", "/oauth2/**", "/login/**", "/join/**","/user/**","/admin/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(
                                                sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/view/home",
                                                                "/view/signin",
                                                                "/view/login",
                                                                "/view/register",
                                                                "/view/forgot-password",
                                                                "/view/reset-password",
                                                                "/view/password-reset-success",
                                                                "/view/verify-otp",
                                                                "/join/**",
                                                                "/css/**", "/js/**", "/images/**", "/assets/**",
                                                                "/favicon.ico", "/webjars/**",
                                                                "/oauth2/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/user/**").authenticated()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .loginPage("/view/signin")
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureUrl("/view/signin?error=oauth"))
                                .formLogin(form -> form.disable())
                                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                                        String redirect = req.getRequestURI();
                                        res.setStatus(HttpServletResponse.SC_FOUND);
                                        res.setHeader("Location", "/view/signin?redirect=" + redirect);
                                }))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/view/signin?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("AUTH_TOKEN", "REFRESH_TOKEN")
                                                .permitAll())
                                .addFilterAfter((request, response, chain) -> {
                                        if (response instanceof HttpServletResponse httpResp) {
                                                httpResp.setHeader("Cache-Control",
                                                                "no-cache, no-store, must-revalidate");
                                                httpResp.setHeader("Pragma", "no-cache");
                                                httpResp.setHeader("Expires", "0");
                                        }
                                        chain.doFilter(request, response);
                                }, SecurityContextHolderAwareRequestFilter.class)
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}