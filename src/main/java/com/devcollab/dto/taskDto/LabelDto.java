package com.devcollab.dto.taskDto;

import com.devcollab.domain.Label;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelDto {
    private Long labelId;
    private String name;
    private String color;

    public static LabelDto fromEntity(Label label) {
        return new LabelDto(
                label.getLabelId(),
                label.getName(),
                label.getColor()
        );
    }
}
