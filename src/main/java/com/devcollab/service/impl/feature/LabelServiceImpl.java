package com.devcollab.service.impl.feature;

import com.devcollab.domain.Label;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.dto.LabelDTO;
import com.devcollab.repository.LabelRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.LabelService;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class LabelServiceImpl implements LabelService {

    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public LabelServiceImpl(LabelRepository labelRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository) {
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<LabelDTO> getLabelsByProject(Long projectId, String keyword) {
        return labelRepository.findByProjectAndKeyword(projectId, keyword)
                .stream()
                .map(l -> new LabelDTO(l.getLabelId(), l.getName(), l.getColor()))
                .collect(Collectors.toList());
    }

    @Override
    public LabelDTO createLabel(Long projectId, String name, String color) {
        if (labelRepository.existsByProject_ProjectIdAndNameIgnoreCase(projectId, name))
            throw new IllegalArgumentException("Label name already exists in this project");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Label label = new Label();
        label.setProject(project);
        label.setName(name);
        label.setColor(color != null ? color : "#61bd4f");

        Label saved = labelRepository.save(label);
        return new LabelDTO(saved.getLabelId(), saved.getName(), saved.getColor());
    }

    @Override
    public LabelDTO updateLabel(Long labelId, String name, String color) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));
        if (name != null && !name.isBlank())
            label.setName(name);
        if (color != null && !color.isBlank())
            label.setColor(color);

        Label saved = labelRepository.save(label);
        return new LabelDTO(saved.getLabelId(), saved.getName(), saved.getColor());
    }

    @Override
    @Transactional
    public void deleteLabel(Long labelId) {
        // 1️⃣ Xóa toàn bộ bản ghi liên kết trong bảng TaskLabel
        labelRepository.deleteAllTaskRelations(labelId);

        // 2️⃣ Sau đó mới xóa label chính
        labelRepository.deleteById(labelId);
    }

    @Override
    @Transactional(readOnly = true)
    public LabelDTO getLabelById(Long labelId) {
        LabelDTO dto = labelRepository.findDtoById(labelId);
        if (dto == null) {
            throw new IllegalArgumentException("Label not found");
        }
        return dto;
    }

    @Override
    public List<LabelDTO> getLabelsByTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Hibernate.initialize(task.getLabels());

        return task.getLabels().stream()
                .map(l -> new LabelDTO(l.getLabelId(), l.getName(), l.getColor()))
                .collect(Collectors.toList());
    }
    @Override
    public void assignLabelToTask(Long taskId, Long labelId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));

        // tránh duplicate
        boolean exists = task.getLabels().stream()
                .anyMatch(l -> Objects.equals(l.getLabelId(), labelId));
        if (!exists) task.getLabels().add(label);

        taskRepository.save(task);
    }

    @Override
    public void removeLabelFromTask(Long taskId, Long labelId) {
        if (!taskRepository.existsById(taskId) || !labelRepository.existsById(labelId)) {
            throw new IllegalArgumentException("Task or Label not found");
        }
        labelRepository.deleteTaskLabel(taskId, labelId);
    }
    

}   
