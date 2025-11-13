package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.Label;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.LabelDTO;
import com.devcollab.repository.LabelRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.LabelService;
import com.devcollab.service.system.ProjectAuthorizationService;
import org.hibernate.Hibernate;
import org.springframework.security.access.AccessDeniedException;
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
    private final TaskFollowerRepository followerRepo;

    public LabelServiceImpl(LabelRepository labelRepository, ProjectRepository projectRepository,
            TaskRepository taskRepository, TaskFollowerRepository followerRepo) {
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.followerRepo = followerRepo;
    }

    @Override
    public List<LabelDTO> getLabelsByProject(Long projectId, String keyword) {
        return labelRepository.findByProjectAndKeyword(projectId, keyword);
    }

    @Override
    public LabelDTO createLabel(Long projectId, String name, String color, User actor) {

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isMember = authz.isMemberOfProject(actor.getEmail(), projectId);

        if (!isPm && !isMember) {
            throw new AccessDeniedException(
                    "Chỉ PM/ADMIN hoặc thành viên dự án mới được tạo label.");
        }

        if (labelRepository.existsByProject_ProjectIdAndNameIgnoreCase(projectId, name))
            throw new IllegalArgumentException("Label name already exists in this project");

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Label label = new Label();
        label.setProject(project);
        label.setName(name);
        label.setColor(color != null ? color : "#61bd4f");
        label.setCreatedBy(actor); // 🔥 người tạo label

        Label saved = labelRepository.save(label);
        return new LabelDTO(saved.getLabelId(), saved.getName(), saved.getColor(), actor.getUserId(),
                actor.getName());
    }

    @Override
    public LabelDTO updateLabel(Long labelId, String name, String color, User actor) {

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));

        Long projectId = label.getProject().getProjectId();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isCreator = (label.getCreatedBy() != null
                && label.getCreatedBy().getUserId().equals(actor.getUserId()));

        if (!isPm && !isCreator) {
            throw new AccessDeniedException("Chỉ PM/ADMIN hoặc người tạo label mới được cập nhật.");
        }

        if (name != null && !name.isBlank())
            label.setName(name);
        if (color != null && !color.isBlank())
            label.setColor(color);

        Label saved = labelRepository.save(label);
        return new LabelDTO(saved.getLabelId(), saved.getName(), saved.getColor(),
                saved.getCreatedBy() != null ? saved.getCreatedBy().getUserId() : null,
                saved.getCreatedBy() != null ? saved.getCreatedBy().getName() : null);
    }


    @Override
    @Transactional
    public void deleteLabel(Long labelId, User actor) {

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));

        Long projectId = label.getProject().getProjectId();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isCreator = (label.getCreatedBy() != null
                && label.getCreatedBy().getUserId().equals(actor.getUserId()));

        if (!isPm && !isCreator) {
            throw new AccessDeniedException("Chỉ PM/ADMIN hoặc người tạo label mới được xóa.");
        }
        labelRepository.deleteAllTaskRelations(labelId);

        labelRepository.delete(label);
    }

    @Override
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
                .map(l -> new LabelDTO(l.getLabelId(), l.getName(), l.getColor(),
                        l.getCreatedBy() != null ? l.getCreatedBy().getUserId() : null,
                        l.getCreatedBy() != null ? l.getCreatedBy().getName() : null))
                .collect(Collectors.toList());
    }

    @Override
    public void assignLabelToTask(Long taskId, Long labelId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isFollower = followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, actorId);

        if (!isPm && !isFollower) {
            throw new AccessDeniedException("Bạn không đủ quyền gán label vào task.");
        }

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new IllegalArgumentException("Label not found"));

        boolean exists =
                task.getLabels().stream().anyMatch(l -> Objects.equals(l.getLabelId(), labelId));

        if (!exists)
            task.getLabels().add(label);

        taskRepository.save(task);
    }

    @Override
    public void removeLabelFromTask(Long taskId, Long labelId, User actor) {

        Long actorId = actor.getUserId();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        Long projectId = task.getProject().getProjectId();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isFollower = followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, actorId);

        if (!isPm && !isFollower) {
            throw new AccessDeniedException("Bạn không đủ quyền xóa label khỏi task.");
        }

        labelRepository.deleteTaskLabel(taskId, labelId);
    }
}
