package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.service.core.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final Cloudinary cloudinary;

    public UserRestController(UserService userService, PasswordEncoder passwordEncoder, Cloudinary cloudinary) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.cloudinary = cloudinary;
    }

    @GetMapping
    public List<UserDTO> getAllUsers() {
        System.out.println("DEBUG: getAllUsers called");
        return userService.getAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        System.out.println("DEBUG: getUser called with id=" + id);
        return userService.getById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User not found id=" + id));
    }

    @PostMapping
    public UserDTO createUser(@RequestBody UserDTO dto) {
        System.out.println("DEBUG: createUser called with dto=" + dto);
        try {
            User user = toEntity(dto);
            return toDto(userService.create(user));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/{id}")
    public UserDTO updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        System.out.println("DEBUG: updateUser called with id=" + id + " dto=" + dto);
        try {
            User existing = userService.getById(id)
                    .orElseThrow(() -> new RuntimeException("User not found id=" + id));

            existing.setEmail(dto.getEmail());
            existing.setName(dto.getName());
            existing.setStatus(dto.getStatus());
            existing.setSkills(dto.getSkills());
            existing.setAvatarUrl(dto.getAvatarUrl());
            existing.setBio(dto.getBio());
            existing.setTimezone(dto.getTimezone());
            existing.setPreferredLanguage(dto.getPreferredLanguage());
            existing.setUpdatedAt(LocalDateTime.now());

            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                existing.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            }

            User updatedUser = userService.update(id, existing);
            return toDto(updatedUser);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        System.out.println("DEBUG: inactive User called with id=" + id);
        try {
            userService.inactivate(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // --- Mapping helpers ---

    private UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setStatus(user.getStatus());
        dto.setSkills(user.getSkills());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setTimezone(user.getTimezone());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    private User toEntity(UserDTO dto) {
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setStatus(dto.getStatus());
        user.setSkills(dto.getSkills());
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setBio(dto.getBio());
        user.setPreferredLanguage(dto.getPreferredLanguage());
        user.setTimezone(dto.getTimezone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPasswordHash(dto.getPassword());
        }

        return user;
    }

    @PostMapping("/{id}/avatar")
    public UserDTO uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("DEBUG: uploadAvatar called with id=" + id + " file=" + file.getOriginalFilename());

        // Upload lên Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "devcollab/avatars"));

        String imageUrl = uploadResult.get("secure_url").toString();

        // Cập nhật URL cho user
        User user = userService.getById(id)
                .orElseThrow(() -> new RuntimeException("User not found id=" + id));

        user.setAvatarUrl(imageUrl);
        userService.update(id, user);

        // Trả về thông tin user mới
        return toDto(user);
    }

    @PostMapping("/{id}/change-password")
    public Map<String, Object> changePassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        System.out.println("DEBUG: changePassword called for userId=" + id);

        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        User user = userService.getById(id)
                .orElseThrow(() -> new RuntimeException("User not found id=" + id));

        // 1️⃣ Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác");
        }

        // 2️⃣ Kiểm tra mật khẩu mới và xác nhận
        if (newPassword == null || newPassword.isEmpty() || !newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu mới và xác nhận không khớp");
        }

        // ✅ 3️⃣ Kiểm tra độ mạnh mật khẩu
        String passwordPattern = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).{8,}$";
        if (!newPassword.matches(passwordPattern)) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, số và ký tự đặc biệt.");
        }

        // 4️⃣ Cập nhật mật khẩu
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userService.updatePassword(user.getEmail(), newPassword);
        return Map.of("message", "Đổi mật khẩu thành công", "status", "success");
    }

}