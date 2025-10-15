package com.devcollab.dto;

import com.devcollab.domain.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDto {

    private Long userId;
    private String email;
    private String name;
    private String skills;
    private String status;
    private String avatarUrl;
    private String bio;
    private String preferredLanguage;
    private String timezone;

    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDto(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.skills = user.getSkills();
        this.status = user.getStatus();
        this.avatarUrl = user.getAvatarUrl();
        this.bio = user.getBio();
        this.preferredLanguage = user.getPreferredLanguage();
        this.timezone = user.getTimezone();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    public UserDto() {
    }
}
