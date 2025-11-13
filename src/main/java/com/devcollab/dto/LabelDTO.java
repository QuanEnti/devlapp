package com.devcollab.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LabelDTO {
    private Long labelId;
    private String name;
    private String color;
    private Long createdById;
    private String createdByName;

    public LabelDTO(Long labelId, String name, String color) {
        this.labelId = labelId;
        this.name = name;
        this.color = color;
    }

    public LabelDTO(Long labelId, String name, String color, Long createdById,
            String createdByName) {
        this.labelId = labelId;
        this.name = name;
        this.color = color;
        this.createdById = createdById;
        this.createdByName = createdByName;
    }
}
