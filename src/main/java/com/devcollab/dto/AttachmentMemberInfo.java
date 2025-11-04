package com.devcollab.dto;

public class AttachmentMemberInfo {
    private Long userId;
    private String name;
    private String avatarUrl;

    public AttachmentMemberInfo(Long userId, String name, String avatarUrl) {
        this.userId = userId;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
