package com.devcollab.dto.UserProfileDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ProjectSummaryDto {
    private Long projectId;
    private String name;
}
