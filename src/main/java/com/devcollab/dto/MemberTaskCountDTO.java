package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTaskCountDTO {
    private Long userId;
    private String name;
    private String email;
    private Long taskCount;
}


