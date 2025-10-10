package com.devcollab.security;

import com.devcollab.domain.User;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.redirect}")
    private String frontendRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        DefaultOAuth2User oauthUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatar = oauthUser.getAttribute("picture");
        String providerId = oauthUser.getAttribute("sub"); 

        if (email == null || email.isEmpty()) {
            response.sendRedirect("/signin.html?error=missing_email");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "Google User");
            newUser.setAvatarUrl(avatar);
            newUser.setProvider("google");
            newUser.setProviderId(providerId);
            newUser.setPasswordHash(null); 
            newUser.setStatus("verified");
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        user.setLastSeen(LocalDateTime.now());
        if (avatar != null && !avatar.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatar);
        }
        userRepository.save(user);

        String token = jwtService.generateToken(email);

        response.sendRedirect(frontendRedirect + "?token=" + token);
    }
}
