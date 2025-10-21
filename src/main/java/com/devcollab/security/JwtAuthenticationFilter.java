package com.devcollab.security;

import com.devcollab.service.impl.core.UserServiceImpl;
import com.devcollab.service.system.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserServiceImpl userService;

    private final JwtTokenProvider jwtTokenProvider;

//    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtService jwtService, UserServiceImpl userService, JwtTokenProvider jwtTokenProvider1) {
//        this.jwtService = jwtService;
//        this.userService = userService;
//        this.jwtTokenProvider = jwtTokenProvider1;
//    }
//
//    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.endsWith(".html")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/favicon")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String email = jwtService.extractEmail(jwt);
        Long userId = jwtService.extractUserId(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtService.isValid(jwt)) {
                UserDetails userDetails = userService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);


                // ✅ Gắn userId vào request (phục vụ cho /me/profile)

                if (userId != null) {
                    request.setAttribute("userId", userId);
                }

                HttpSession session = request.getSession(true);
                if (session.getAttribute("userEmail") == null) {
                    session.setAttribute("userEmail", email);
                    session.setAttribute("roles", userDetails.getAuthorities());
                }
            }
        }




        filterChain.doFilter(request, response);
    }
}
