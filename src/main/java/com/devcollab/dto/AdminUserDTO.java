package com.devcollab.dto;

import com.devcollab.domain.User;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDTO {

    private Long userId;
    private String email;
    private String name;
    private String avatarUrl;
    private String status;
    private String skills;
    private String bio;
    private String provider;
    private String preferredLanguage;
    private String timezone;

    private boolean isPremium;
    private Instant premiumExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;

    private List<String> roles;

    // ✅ Map trực tiếp từ entity User
    public static AdminUserDTO fromEntity(User user) {
        if (user == null) return null;

        return AdminUserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .skills(user.getSkills())
                .bio(user.getBio())
                .provider(user.getProvider())
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .isPremium(user.isPremium())
                .premiumExpiry(user.getPremiumExpiry())
                .createdAt(user.getCreatedAt())
                .lastSeen(user.getLastSeen())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toList()))
                .build();
    }
}