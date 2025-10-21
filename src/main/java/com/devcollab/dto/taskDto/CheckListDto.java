package com.devcollab.dto.taskDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.devcollab.domain.CheckList;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckListDto {
    private Long checklistId;
    private String item;
    private boolean isDone;
    private int orderIndex;

    public static CheckListDto fromEntity(CheckList checklist) {
        return new CheckListDto(
                checklist.getChecklistId(),
                checklist.getItem(),
                checklist.getIsDone(),
                checklist.getOrderIndex()
        );
    }
}
