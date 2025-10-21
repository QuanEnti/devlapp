package com.devcollab.dto.UserProfileDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSumaryDto {
    private Long taskId;
    private String title;
}
