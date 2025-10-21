package com.devcollab.dto.taskDto;

import com.devcollab.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long userId;
    private String name;
    private String avatarUrl;

    public static UserSummaryDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryDto(
                user.getUserId(),
                user.getName(),
                user.getAvatarUrl()
        );
    }
}
