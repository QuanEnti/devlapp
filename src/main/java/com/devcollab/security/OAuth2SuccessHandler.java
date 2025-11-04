
package com.devcollab.security;

import com.devcollab.domain.User;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
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
        String initialAvatar = oauthUser.getAttribute("picture");
        String providerId = oauthUser.getAttribute("sub");

        if (email == null || email.isEmpty()) {
            response.sendRedirect("/signin.html?error=missing_email");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name != null ? name : "Google User");
            newUser.setAvatarUrl(initialAvatar);
            newUser.setProvider("google");
            newUser.setProviderId(providerId);
            newUser.setStatus("verified");
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(newUser);
        });

        user.setLastSeen(LocalDateTime.now());

        String avatar = initialAvatar;
        if (avatar == null || avatar.isBlank() || !avatar.startsWith("https://lh3.googleusercontent.com/")) {
            avatar = "https://ui-avatars.com/api/?name=" + (name != null ? name.replace(" ", "+") : "User");
        }

        if (user.getAvatarUrl() == null || !user.getAvatarUrl().equals(avatar)) {
            user.setAvatarUrl(avatar);
            user.setUpdatedAt(LocalDateTime.now());
        }

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken(email);

        ResponseCookie accessCookie = ResponseCookie.from("AUTH_TOKEN", accessToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(15 * 60)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect(frontendRedirect);
    }
}
