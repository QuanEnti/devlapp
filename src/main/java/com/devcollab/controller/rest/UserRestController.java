package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.dto.AdminUserDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.time.LocalDateTime;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    // ============================
    // GET ALL USERS (ADMIN)
    // ============================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserDTO> getAllUsersForAdmin() {
        return userService.getAll().stream()
                .map(AdminUserDTO::fromEntity)
                .toList();
    }

    // ============================
    // GET ONE USER
    // ============================
    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userService.getById(id)
                .map(UserDTO::new)
                .orElseThrow(() -> new NotFoundException("User not found id=" + id));
    }

    // ============================
    // UPDATE USER PROFILE (SELF ONLY)
    // ============================
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public UserDTO updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO dto,
            Authentication auth) {
        // 👇 Convert UserDTO → User patch entity
        User patch = new User();
        patch.setName(dto.getName());
        patch.setAvatarUrl(dto.getAvatarUrl());
        patch.setBio(dto.getBio());
        patch.setSkills(dto.getSkills());
        patch.setPreferredLanguage(dto.getPreferredLanguage());
        patch.setTimezone(dto.getTimezone());
        patch.setStatus(dto.getStatus());

        User updated = userService.update(id, patch, auth);
        return new UserDTO(updated);
    }

    // ============================
    // BAN / UNBAN USER (ADMIN)
    @PostMapping("/{id}/toggle-ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleBan(@PathVariable Long id, Authentication auth) {

        User user = userService.getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // ❗ Không cho admin tự ban chính mình
        UserDTO current = authService.getCurrentUser(auth);
        if (current.getUserId().equals(id)) {
            throw new IllegalArgumentException("Admin không thể tự khóa tài khoản của chính mình.");
        }

        boolean isBanned = "banned".equalsIgnoreCase(user.getStatus())
                || "suspended".equalsIgnoreCase(user.getStatus());

        String newStatus = isBanned ? "active" : "banned";

        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());

        userService.saveAdminOverride(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "newStatus", newStatus,
                "message", isBanned ? "User unbanned successfully" : "User banned successfully"));
    }

    // ============================
    // UPLOAD AVATAR (SELF ONLY)
    // ============================
    @PostMapping("/{id}/avatar")
    @PreAuthorize("isAuthenticated()")
    public UserDTO uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(), ObjectUtils.asMap("folder", "devcollab/avatars"));

        String url = uploadResult.get("secure_url").toString();

        User patch = new User();
        patch.setAvatarUrl(url);

        User updated = userService.update(id, patch, auth);
        return new UserDTO(updated);
    }

    // ============================
    // CHANGE PASSWORD (SELF ONLY)
    // ============================
    @PostMapping("/{id}/change-password")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication auth) {

        User user = userService.getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Chỉ chính chủ được đổi password
        if (!authService.getCurrentUser(auth).getUserId().equals(id)) {
            throw new SecurityException("Bạn không thể đổi mật khẩu của người khác.");
        }

        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu mới không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());

        userService.updatePassword(user.getEmail(), newPassword);

        return Map.of("status", "success", "message", "Đổi mật khẩu thành công");
    }

    // ============================
    // GET PROFILE PAGE
    // ============================
    @GetMapping("/{userId}/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        var user = userService.getById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return ResponseEntity.ok(new UserDTO(user));
    }

    // ============================
    // GET USER BY EMAIL
    // ============================
    @GetMapping("/by-email/{email}")
    public ResponseEntity<?> getByEmail(@PathVariable String email) {

        var user = userService.getByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var dto = new UserDTO(user);

        return ResponseEntity.ok(Map.of(
                "userId", dto.getUserId(),
                "name", dto.getName(),
                "email", dto.getEmail(),
                "avatarUrl", dto.getAvatarUrl(),
                "bio", dto.getBio(),
                "status", dto.getStatus(),
                "provider", dto.getProvider()));
    }
}
