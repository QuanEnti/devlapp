package com.devcollab.service.impl.feature;

import com.devcollab.domain.Label;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.LabelRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.LabelService;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ActivityService activityService;

    @Override
    public List<Label> getLabelsByProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
        return labelRepository.findByProject_ProjectId(projectId);
    }

    @Override
    public Label createLabel(Long projectId, String name, String color) {
        if (name == null || name.isBlank())
            throw new BadRequestException("Tên label không được để trống");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        Label label = new Label();
        label.setProject(project);
        label.setName(name.trim());
        label.setColor(color != null && !color.isBlank() ? color : "#888888");

        try {
            Label saved = labelRepository.save(label);
            activityService.log("LABEL", saved.getLabelId(), "CREATE", saved.getName());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Tên label đã tồn tại trong dự án này");
        }
    }

    @Override
    public Label updateLabel(Long labelId, String name, String color) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy label"));

        if (name != null && !name.isBlank())
            label.setName(name.trim());
        if (color != null && !color.isBlank())
            label.setColor(color);

        try {
            Label saved = labelRepository.save(label);
            activityService.log("LABEL", saved.getLabelId(), "UPDATE", saved.getName());
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Tên label đã tồn tại trong dự án này");
        }
    }

    @Override
    public void deleteLabel(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy label"));

        if (!label.getTasks().isEmpty())
            throw new BadRequestException("Không thể xoá label đang được gán cho task");

        labelRepository.delete(label);
        activityService.log("LABEL", labelId, "DELETE", "Hard delete");
    }

    @Override
    public void assignLabelToTask(Long taskId, Long labelId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy label"));

        if (!task.getProject().getProjectId().equals(label.getProject().getProjectId()))
            throw new BadRequestException("Label và Task không cùng thuộc một dự án");

        boolean exists = task.getLabels().stream()
                .anyMatch(l -> l.getLabelId().equals(labelId));
        if (exists)
            throw new BadRequestException("Task đã có label này rồi");

        task.getLabels().add(label);
        taskRepository.save(task);

        activityService.log("TASK", taskId, "ASSIGN_LABEL", label.getName());
    }

    @Override
    public void removeLabelFromTask(Long taskId, Long labelId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        boolean removed = task.getLabels().removeIf(l -> l.getLabelId().equals(labelId));
        if (!removed)
            throw new NotFoundException("Label không tồn tại trong task này");

        taskRepository.save(task);
        activityService.log("TASK", taskId, "REMOVE_LABEL", "Label ID: " + labelId);
    }

    @Override
    public List<Task> getTasksByLabel(Long labelId) {
        labelRepository.findById(labelId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy label"));
        return taskRepository.findByLabels_LabelId(labelId);
    }
}
