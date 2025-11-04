package com.devcollab.dto;

import com.devcollab.domain.BoardColumn;
import lombok.*;
import com.devcollab.domain.Task;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumnDTO {
    private Long columnId;
    private String name;
    private Integer orderIndex;
    private Boolean isDefault;

    public static BoardColumnDTO fromEntity(BoardColumn entity) {
        return BoardColumnDTO.builder()
                .columnId(entity.getColumnId())
                .name(entity.getName())
                .orderIndex(entity.getOrderIndex())
                .isDefault(entity.getIsDefault())
                .build();
    }

    public BoardColumn toEntity() {
        BoardColumn col = new BoardColumn();
        col.setColumnId(this.columnId);
        col.setName(this.name);
        col.setOrderIndex(this.orderIndex);
        col.setIsDefault(this.isDefault);
        return col;
    }
}