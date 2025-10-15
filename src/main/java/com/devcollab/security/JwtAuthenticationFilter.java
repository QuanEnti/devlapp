package com.devcollab.security;

import com.devcollab.service.impl.core.UserServiceImpl;
import com.devcollab.service.system.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserServiceImpl userService;

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserServiceImpl userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromRequest(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractEmail(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isValid(jwt)) {
                    UserDetails userDetails = userService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    HttpSession session = request.getSession(true);
                    session.setAttribute("userEmail", email);
                    session.setAttribute("roles", userDetails.getAuthorities());
                }
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && isPublicAuthPage(path)) {
                response.sendRedirect("/view/home");
                return;
            }

            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

        } catch (ExpiredJwtException ex) {
            handleJwtError(response, path, "Token expired", "JWT đã hết hạn, vui lòng đăng nhập lại.",
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception ex) {
            handleJwtError(response, path, "Invalid token", "Token không hợp lệ hoặc bị lỗi.",
                    HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleJwtError(HttpServletResponse response, String path, String error, String message, int statusCode)
            throws IOException {
        if (path.startsWith("/api/")) {
            response.setStatus(statusCode);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
        } else {
            if (error.equals("Token expired")) {
                response.sendRedirect("/view/signin?expired");
            } else {
                response.sendRedirect("/view/signin?invalid");
            }
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/api/auth/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/favicon")
                || path.startsWith("/webjars/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/oauth2/");
    }


    private boolean isPublicAuthPage(String path) {
        return path.startsWith("/view/login")
                || path.startsWith("/view/signin")
                || path.startsWith("/view/register")
                || path.startsWith("/view/forgot-password")
                || path.startsWith("/view/reset-password")
                || path.startsWith("/view/verify-otp");
    }
}
