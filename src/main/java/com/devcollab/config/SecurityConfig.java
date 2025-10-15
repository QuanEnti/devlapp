package com.devcollab.config;

import com.devcollab.security.JwtAuthenticationFilter;
import com.devcollab.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        @Bean
        @Order(1)
        public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/api/**")
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth                            
                                                .requestMatchers("/api/auth/**", "/api/users/**", "/api/admin/**","api/pm/**","user/**").permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout.disable());

                return http.build();
        }
        
        @Bean
        @Order(2)
        public SecurityFilterChain viewSecurity(HttpSecurity http) throws Exception {
                http
                                .securityMatcher("/view/**", "/", "/login/**", "/oauth2/**")
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", "/view/**",
                                                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                                                "/webjars/**",
                                                                "/oauth2/**", "/login/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .loginPage("/view/signin")
                                                .authorizationEndpoint(
                                                                config -> config.baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(config -> config.baseUri("/login/oauth2/code/*"))
                                                .successHandler(oAuth2SuccessHandler)
                                                .failureUrl("/view/signin?error=oauth"))
                                .formLogin(form -> form
                                                .loginPage("/view/signin")
                                                .defaultSuccessUrl("/view/dashboard", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/view/signin?logout")
                                                .permitAll());

                return http.build();
        }

      
}
