package com.devcollab.dto.UserProfileDto;

import com.devcollab.domain.profileEnity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollaboratorDto {
    private Long userId;
    private String name;
    private String avatarUrl;

    public static CollaboratorDto fromEntity(User user) {
        return new CollaboratorDto(
                user.getUserId(),
                user.getName(),
                user.getAvatarUrl()
        );
    }
}
