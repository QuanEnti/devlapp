package com.devcollab.service.feature;

import com.devcollab.domain.Label;
import com.devcollab.domain.Task;
import java.util.List;

public interface LabelService {

    List<Label> getLabelsByProject(Long projectId);

    Label createLabel(Long projectId, String name, String color);

    Label updateLabel(Long labelId, String name, String color);

    void deleteLabel(Long labelId);

    void assignLabelToTask(Long taskId, Long labelId);

    void removeLabelFromTask(Long taskId, Long labelId);

    List<Task> getTasksByLabel(Long labelId);
}
