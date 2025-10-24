package com.devcollab.service.impl.system;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserDTO getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("⚠️ Attempted to access current user without authentication");
            throw new SecurityException("Unauthorized");
        }

        Object principal = auth.getPrincipal();

        // ✅ 1. Google OAuth2 Login
        if (principal instanceof OAuth2User oauthUser) {
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("name");
            String avatar = oauthUser.getAttribute("picture");

            if (email == null || email.isBlank()) {
                log.error("❌ Missing email attribute from OAuth2 user: {}", oauthUser);
                throw new IllegalStateException("Missing email attribute from Google account");
            }

            // Nếu user đã tồn tại trong DB thì lấy thông tin từ DB
            var existingUserOpt = userRepository.findByEmail(email);
            if (existingUserOpt.isPresent()) {
                var user = existingUserOpt.get();
                log.debug("✅ Authenticated via Google OAuth2 (existing user): {}", email);
                return UserDTO.fromEntity(user);
            }

            // Nếu chưa tồn tại, tạo tạm DTO từ OAuth info
            log.debug("✅ Authenticated via Google OAuth2 (new user session): {}", email);
            return UserDTO.builder()
                    .userId(null)
                    .name(name != null ? name : "Google User")
                    .email(email)
                    .avatarUrl(avatar != null ? avatar : "https://i.pravatar.cc/100?u=" + email)
                    .provider("google")
                    .build();
        }

        // ✅ 2. Local Login (JWT)
        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            var userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.error("❌ User not found in DB: {}", email);
                throw new IllegalArgumentException("USER_NOT_FOUND");
            }

            var user = userOpt.get();
            log.debug("✅ Authenticated via local account: {}", email);

            return UserDTO.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl() != null
                            ? user.getAvatarUrl()
                            : "https://i.pravatar.cc/100?u=" + user.getEmail())
                    .provider(user.getProvider() != null ? user.getProvider() : "local")
                    .build();
        }

        // ✅ 3. Unsupported Type
        String type = principal != null ? principal.getClass().getSimpleName() : "null";
        log.error("❌ Unsupported authentication principal type: {}", type);
        throw new IllegalArgumentException("Unsupported authentication type: " + type);
    }
}
