package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDTO {
    private Long userId;
    private String name;
    private String avatarUrl;
    private String color;
}

