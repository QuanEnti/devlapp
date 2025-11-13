package com.devcollab.service.feature;

import com.devcollab.domain.User;
import com.devcollab.dto.LabelDTO;
import java.util.List;

public interface LabelService {
    List<LabelDTO> getLabelsByProject(Long projectId, String keyword);

    List<LabelDTO> getLabelsByTask(Long taskId);

    LabelDTO createLabel(Long projectId, String name, String color, User actor);

    LabelDTO updateLabel(Long labelId, String name, String color, User actor);

    void deleteLabel(Long labelId, User actor);

    void assignLabelToTask(Long taskId, Long labelId, User actor);

    void removeLabelFromTask(Long taskId, Long labelId, User actor);

    LabelDTO getLabelById(Long labelId);


}
