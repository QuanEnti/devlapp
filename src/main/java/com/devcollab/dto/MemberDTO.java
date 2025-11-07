package com.devcollab.dto;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
public class MemberDTO {
    private Long userId;
    private String name;
    private String email;
    private String avatarUrl;
    private String roleInProject;

    public MemberDTO(Long userId, String name, String avatarUrl) {
        this.userId = userId;
        this.name = name;
        this.avatarUrl = avatarUrl;
    }

    public MemberDTO(Long userId, String name, String email, String avatarUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }
    public MemberDTO(Long userId, String name, String email, String avatarUrl, String roleInProject) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.roleInProject = roleInProject;
    }

}
