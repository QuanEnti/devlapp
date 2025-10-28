package com.devcollab.service.feature;

import com.devcollab.dto.LabelDTO;
import java.util.List;

public interface LabelService {
    List<LabelDTO> getLabelsByProject(Long projectId, String keyword);

    List<LabelDTO> getLabelsByTask(Long taskId);

    LabelDTO createLabel(Long projectId, String name, String color);

    LabelDTO updateLabel(Long labelId, String name, String color);

    void deleteLabel(Long labelId);

    void assignLabelToTask(Long taskId, Long labelId);

    void removeLabelFromTask(Long taskId, Long labelId);

    LabelDTO getLabelById(Long labelId);


}
