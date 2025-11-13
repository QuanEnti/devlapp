package com.devcollab.service.impl.core;

import com.devcollab.domain.User;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.UserService;
import com.devcollab.service.event.AppEventService;
// import com.devcollab.service.system.NotificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final AppEventService appEventService;
    private final PasswordEncoder passwordEncoder;
    // private final NotificationService notificationService;

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User create(User user) {
        Optional<User> existingOpt = userRepository.findByEmail(user.getEmail());

        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();

            if ("google".equalsIgnoreCase(existing.getProvider()) && user.getPasswordHash() != null) {
                existing.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
                existing.setProvider("local_google");
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setLastSeen(LocalDateTime.now());
                return userRepository.save(existing);
            }

            if ("local".equalsIgnoreCase(existing.getProvider()) ||
                    "local_google".equalsIgnoreCase(existing.getProvider())) {
                throw new BadRequestException("Email này đã có tài khoản mật khẩu");
            }

            if ("otp".equalsIgnoreCase(existing.getProvider()) && user.getPasswordHash() != null) {
                existing.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
                existing.setProvider("local");
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setLastSeen(LocalDateTime.now());
                return userRepository.save(existing);
            }

            throw new BadRequestException("Email đã tồn tại trong hệ thống");
        }
        user.setProvider(user.getProvider() != null ? user.getProvider() : "local");
        user.setStatus(user.getStatus() != null ? user.getStatus() : "unverified");

        if (user.getPasswordHash() != null && !user.getPasswordHash().startsWith("$2a")) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        User saved = userRepository.save(user);

        appEventService.publishUserCreated(saved);
        return saved;
    }

    @Override
    public User update(Long id, User patch) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với ID: " + id));

        if (patch.getName() != null)
            existing.setName(patch.getName());
        if (patch.getAvatarUrl() != null)
            existing.setAvatarUrl(patch.getAvatarUrl());
        if (patch.getBio() != null)
            existing.setBio(patch.getBio());
        if (patch.getSkills() != null)
            existing.setSkills(patch.getSkills());
        if (patch.getPreferredLanguage() != null)
            existing.setPreferredLanguage(patch.getPreferredLanguage());
        if (patch.getTimezone() != null)
            existing.setTimezone(patch.getTimezone());
        if (patch.getStatus() != null)
            existing.setStatus(patch.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(existing);
        appEventService.publishUserStatusChanged(saved);
        // notificationService.notifyChangeProfile(saved); Khi sửa hồ sơ thì không cần thông báo
        return saved;
    }

    @Override
    public User updateStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với ID: " + id));

        if (!status.matches("active|suspended|deleted")) {
            throw new BadRequestException("Trạng thái không hợp lệ");
        }

        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        appEventService.publishUserStatusChanged(saved);
        return saved;
    }

    @Override
    public void updateLastSeen(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setLastSeen(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User không tồn tại");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void inactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found id=" + id));

        user.setStatus("Suspended"); // mark user inactive
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean checkPassword(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }

    @Override
    public void markVerified(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String currentStatus = user.getStatus() != null ? user.getStatus().toLowerCase() : "unknown";
            System.out.println("User Status: " + currentStatus);
            // 🚫 Do NOT overwrite banned or suspended users
            if ("banned".equals(currentStatus) || "suspended".equals(currentStatus)) {
                return;
            }
            // ✅ Only set to verified if not already
            if (!"verified".equals(currentStatus)) {
                user.setStatus("verified");
            }
            // 🧩 Update provider if necessary
            if ("otp".equalsIgnoreCase(user.getProvider())) {
                user.setProvider("local");
            }
            user.setUpdatedAt(LocalDateTime.now());
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
        });
    }


    @Override
    public void updatePassword(String email, String newPassword) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            if (!"local".equalsIgnoreCase(user.getProvider()) &&
                    !"local_google".equalsIgnoreCase(user.getProvider())) {
                user.setProvider("local");
            }
            userRepository.save(user);
            // notificationService.notifyChangePassword(user); Khi đổi mật khẩu thì không cần thông báo
        });
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 🔍 Dùng query có JOIN FETCH để load roles cùng lúc
        User user = userRepository.findByEmailFetchRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        // ✅ Convert roles trong DB sang GrantedAuthority
        var authorities = user.getRoles().stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                .toList();

        // 🧠 Logging để kiểm tra roles thực tế
        System.out.println("🎯 Loaded user: " + email + " | Roles: " + authorities);

        // ✅ Tạo UserDetails với roles thật từ DB
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!("active".equalsIgnoreCase(user.getStatus())
                        || "verified".equalsIgnoreCase(user.getStatus())))

                .build();
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    @Override
    public long countByStatus(String status){
        return   userRepository.countByStatus(status);
    }

}
