package com.devcollab.service.impl.system;

import com.devcollab.domain.Role;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;

        @Override
        public Optional<User> getUserByEmail(String email) {
                return userRepository.findByEmailFetchRoles(email);
        }

        @Override
        public UserDTO getCurrentUser(Authentication auth) {
                if (auth == null || !auth.isAuthenticated()) {
                        log.warn("⚠️ Attempted to access current user without authentication");
                        throw new SecurityException("Unauthorized");
                }

                Object principal = auth.getPrincipal();

                // =============== CASE 1: OAuth2 Login (Google) ===============
                if (principal instanceof OAuth2User oauthUser) {
                        String email = oauthUser.getAttribute("email");
                        String name = oauthUser.getAttribute("name");
                        String avatar = oauthUser.getAttribute("picture");

                        if (email == null || email.isBlank()) {
                                log.error("❌ Missing email attribute from OAuth2 user: {}", oauthUser);
                                throw new IllegalStateException("Missing email attribute from Google account");
                        }

                        // ✅ Luôn fetch từ DB để lấy roles thật
                        var existingUserOpt = userRepository.findByEmailFetchRoles(email);
                        if (existingUserOpt.isPresent()) {
                                var user = existingUserOpt.get();

                                // Nếu user Google chưa có provider hoặc avatar → cập nhật thêm
                                boolean changed = false;
                                if (user.getProvider() == null || !user.getProvider().equalsIgnoreCase("google")) {
                                        user.setProvider("google");
                                        changed = true;
                                }
                                if (avatar != null && (user.getAvatarUrl() == null
                                                || !avatar.equals(user.getAvatarUrl()))) {
                                        user.setAvatarUrl(avatar);
                                        changed = true;
                                }
                                if (changed)
                                        userRepository.save(user);

                                log.info("✅ Authenticated via Google OAuth2: {} | Roles: {}", email,
                                                user.getRoles().stream().map(Role::getName).toList());
                                return UserDTO.fromEntity(user);
                        }

                        // 🆕 Nếu chưa có user trong DB → tạo mới và gán ROLE_MEMBER mặc định
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setName(name != null ? name : email.split("@")[0]);
                        newUser.setAvatarUrl(avatar);
                        newUser.setProvider("google");
                        newUser.setStatus("verified");

                        newUser = userRepository.save(newUser);
                        log.info("🆕 Created new Google OAuth2 user: {}", email);

                        return UserDTO.fromEntity(newUser);
                }

                // =============== CASE 2: Local Login (Username + Password / OTP)
                // ===============
                if (principal instanceof UserDetails userDetails) {
                        String email = userDetails.getUsername();
                        var userOpt = userRepository.findByEmailFetchRoles(email);

                        if (userOpt.isEmpty()) {
                                log.error("❌ User not found in DB: {}", email);
                                throw new IllegalArgumentException("USER_NOT_FOUND");
                        }

                        var user = userOpt.get();
                        log.info("✅ Authenticated via Local account: {} | Roles: {}", email,
                                        user.getRoles().stream().map(Role::getName).toList());
                        return UserDTO.fromEntity(user);
                }

                // =============== CASE 3: Unsupported ===============
                String type = principal != null ? principal.getClass().getSimpleName() : "null";
                log.error("❌ Unsupported authentication principal type: {}", type);
                throw new IllegalArgumentException("Unsupported authentication type: " + type);
        }

        @Override
        @Transactional
        public User getCurrentUserEntity(Authentication auth) {
                UserDTO dto = getCurrentUser(auth);

                var existing = userRepository.findByEmailFetchRoles(dto.getEmail());
                if (existing.isPresent()) {
                        var user = existing.get();
                        boolean changed = false;

                        if (!user.getName().equals(dto.getName())) {
                                user.setName(dto.getName());
                                changed = true;
                        }
                        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().equals(user.getAvatarUrl())) {
                                user.setAvatarUrl(dto.getAvatarUrl());
                                changed = true;
                        }

                        if (changed) {
                                userRepository.save(user);
                        }

                        return user;
                }

                User newUser = new User();
                newUser.setEmail(dto.getEmail());
                newUser.setName(dto.getName());
                newUser.setAvatarUrl(dto.getAvatarUrl());
                newUser.setProvider(dto.getProvider());
                newUser.setStatus("active");

                User saved = userRepository.save(newUser);
                log.info("🆕 Created new user from OAuth2 login: {}", saved.getEmail());
                return saved;
        }
        
}
