package com.devcollab.dto;

import com.devcollab.domain.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO dùng để truyền dữ liệu người dùng ra bên ngoài (REST API / View)
 * Hỗ trợ cả người dùng Local và OAuth2 (Google)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Data
@Builder
public class UserDTO {

    private Long userId;
    private String email;
    private String name;
    private String avatarUrl;
    private String provider; // 🔹 "local" | "google" | "github" | etc.

    // 🔸 Thông tin mở rộng (chỉ dùng cho entity trong DB)
    private String skills;
    private String status;
    private String bio;
    private String preferredLanguage;
    private String timezone;

    private transient String password; // ⚠️ Không serialize ra JSON (tránh rò rỉ dữ liệu)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🔹 Constructor mapping trực tiếp từ entity User
    public UserDTO(User user) {
        if (user != null) {
            this.userId = user.getUserId();
            this.email = user.getEmail();
            this.name = user.getName();
            this.skills = user.getSkills();
            this.status = user.getStatus();
            this.avatarUrl = user.getAvatarUrl();
            this.bio = user.getBio();
            this.preferredLanguage = user.getPreferredLanguage();
            this.timezone = user.getTimezone();
            this.provider = user.getProvider() != null ? user.getProvider() : "local";
            this.createdAt = user.getCreatedAt();
            this.updatedAt = user.getUpdatedAt();
        }
    }

    // 🔹 Hàm tiện ích: chuyển từ entity sang DTO
    public static UserDTO fromEntity(User user) {
        return user == null ? null : new UserDTO(user);
    }

    // 🔹 Hàm tiện ích: tạo DTO từ dữ liệu Google OAuth2 (email, name, avatar)
    public static UserDTO fromGoogle(String email, String name, String avatar) {
        return UserDTO.builder()
                .userId(null)
                .email(email)
                .name((name != null && !name.isBlank()) ? name : "Unknown User")
                .avatarUrl((avatar != null && !avatar.isBlank())
                        ? avatar
                        : "https://i.pravatar.cc/100?u=" + email)
                .provider("google")
                .build();
    }
}
