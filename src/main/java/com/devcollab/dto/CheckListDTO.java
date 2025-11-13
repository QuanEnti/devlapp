package com.devcollab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckListDTO {

    private Long checklistId;
    private String item;
    private Boolean isDone;
    private Integer orderIndex;
    private Long createdById;
    private String createdByName;

}

