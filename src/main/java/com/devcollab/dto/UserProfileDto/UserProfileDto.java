package com.devcollab.dto.UserProfileDto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private String avatarUrl;
    private String name;
    private String bio;
    private String skills;
    private String email;
    // THAY ĐỔI: Từ List<String> -> List<ProjectSummaryDto>
    private List<ProjectSummaryDto> projects;

    // THAY ĐỔI: Từ List<String> -> List<TaskSummaryDto>
    private List<TaskSumaryDto> tasks;
    private List<CollaboratorDto> collaborators;

}
