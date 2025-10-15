package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;

    private final AppEventService appEventService;
    private final ActivityService activityService;
    private final NotificationService notificationService;

  
    @Override
    public Project createProject(Project project, Long creatorId) {
        if (project == null || project.getName() == null || project.getName().isBlank()) {
            throw new BadRequestException("Tên dự án không được để trống");
        }
        if (project.getStartDate() != null && project.getDueDate() != null
                && project.getDueDate().isBefore(project.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        project.setCreatedBy(creator);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        if (project.getStatus() == null)
            project.setStatus("Active");
        if (project.getPriority() == null)
            project.setPriority("Normal");
        if (project.getVisibility() == null)
            project.setVisibility("private");

        Project saved = projectRepository.save(project);

        ProjectMember pm = new ProjectMember();
        pm.setProject(saved);
        pm.setUser(creator);
        pm.setRoleInProject("PM");
        pm.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(pm);

        String[] defaultCols = { "To-do", "In Progress", "Review", "Done" };
        for (int i = 0; i < defaultCols.length; i++) {
            BoardColumn col = new BoardColumn();
            col.setProject(saved);
            col.setName(defaultCols[i]);
            col.setOrderIndex(i + 1);
            col.setIsDefault(true);
            boardColumnRepository.save(col);
        }

        activityService.log("PROJECT", saved.getProjectId(), "CREATE", saved.getName());
        appEventService.publishProjectCreated(saved);
        notificationService.notifyProjectCreated(saved);

        return saved;
    }

    @Override
    public Project updateProject(Long id, Project patch) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        if (patch.getName() != null && !patch.getName().isBlank())
            existing.setName(patch.getName());
        if (patch.getDescription() != null)
            existing.setDescription(patch.getDescription());
        if (patch.getPriority() != null)
            existing.setPriority(patch.getPriority());
        if (patch.getVisibility() != null)
            existing.setVisibility(patch.getVisibility());
        if (patch.getStartDate() != null)
            existing.setStartDate(patch.getStartDate());
        if (patch.getDueDate() != null) {
            if (existing.getStartDate() != null && patch.getDueDate().isBefore(existing.getStartDate())) {
                throw new BadRequestException("Ngày kết thúc không hợp lệ");
            }
            existing.setDueDate(patch.getDueDate());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        Project saved = projectRepository.save(existing);

        activityService.log("PROJECT", saved.getProjectId(), "UPDATE", saved.getName());
        return saved;
    }

    @Override
    public List<Project> getProjectsByUser(Long userId) {
        List<Project> created = projectRepository.findByCreatedBy_UserId(userId);
        List<ProjectMember> joined = projectMemberRepository.findByUser_UserId(userId);

        Map<Long, Project> all = new LinkedHashMap<>();
        for (Project p : created)
            all.put(p.getProjectId(), p);
        for (ProjectMember m : joined)
            all.put(m.getProject().getProjectId(), m.getProject());
        return new ArrayList<>(all.values());
    }

    @Override
    public ProjectMember addMember(Long projectId, Long userId, String role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Dự án không tồn tại"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        boolean exists = projectMemberRepository.findByProject_ProjectId(projectId)
                .stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (exists) {
            return projectMemberRepository.findByProject_ProjectId(projectId)
                    .stream()
                    .filter(m -> m.getUser().getUserId().equals(userId))
                    .findFirst().get();
        }

        ProjectMember pm = new ProjectMember();
        pm.setProject(project);
        pm.setUser(user);
        pm.setRoleInProject(role != null ? role : "Member");
        pm.setJoinedAt(LocalDateTime.now());
        ProjectMember saved = projectMemberRepository.save(pm);

        activityService.log("PROJECT", projectId, "ADD_MEMBER", user.getEmail());
        appEventService.publishMemberAdded(project, user);
        notificationService.notifyMemberAdded(project, user);
        return saved;
    }

    @Override
    public void removeMember(Long projectId, Long userId) {
        List<ProjectMember> members = projectMemberRepository.findByProject_ProjectId(projectId);
        ProjectMember target = members.stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Thành viên không tồn tại"));

        long pmCount = members.stream()
                .filter(m -> "PM".equalsIgnoreCase(m.getRoleInProject()))
                .count();
        if ("PM".equalsIgnoreCase(target.getRoleInProject()) && pmCount <= 1) {
            throw new BadRequestException("Không thể xóa PM cuối cùng của dự án");
        }

        projectMemberRepository.delete(target);
        activityService.log("PROJECT", projectId, "REMOVE_MEMBER", target.getUser().getEmail());
    }

    @Override
    public Project archiveProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        if (!"Archived".equalsIgnoreCase(project.getStatus())) {
            project.setStatus("Archived");
            project.setArchivedAt(LocalDateTime.now());
            project.setUpdatedAt(LocalDateTime.now());
            project = projectRepository.save(project);

            activityService.log("PROJECT", projectId, "ARCHIVE", project.getName());
            notificationService.notifyProjectArchived(project);
        }
        return project;
    }

    @Override
    public void deleteProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Dự án không tồn tại");
        }
        projectRepository.deleteById(projectId);
        activityService.log("PROJECT", projectId, "DELETE", "Hard delete");
    }
    
    @Override
    public Project getById(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
    }

}
