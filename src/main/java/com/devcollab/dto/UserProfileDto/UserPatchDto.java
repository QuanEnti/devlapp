package com.devcollab.dto.UserProfileDto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPatchDto {
    private String name;
    private String bio;
    private String skills;
    private String avatarUrl;
}
