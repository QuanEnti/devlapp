package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.response.ProjectDashboardDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;
import com.devcollab.dto.response.ProjectSearchResponseDTO;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectAuthorizationService;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectAuthorizationService authz;

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

        String[] defaultCols = {"To-do", "In Progress", "Review", "Done"};
        for (int i = 0; i < defaultCols.length; i++) {
            BoardColumn col = new BoardColumn();
            col.setProject(saved);
            col.setName(defaultCols[i]);
            col.setOrderIndex(i + 1);
            col.setIsDefault(true);
            boardColumnRepository.save(col);
        }

        // activityService.log("PROJECT", saved.getProjectId(), "CREATE",
        // saved.getName());
        appEventService.publishProjectCreated(saved);
        // notificationService.notifyProjectCreated(saved);

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
        for (Project p : created) all.put(p.getProjectId(), p);
        for (ProjectMember m : joined) all.put(m.getProject().getProjectId(), m.getProject());
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

    @Override
    public ProjectDashboardDTO getDashboardForPm(Long projectId, String pmEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        authz.ensurePmOfProject(pmEmail, projectId);

        long total = taskRepository.countByProject_ProjectId(projectId);
        long open = taskRepository.countByProject_ProjectIdAndStatus(projectId, "OPEN");
        long inProgress = taskRepository.countByProject_ProjectIdAndStatus(projectId, "IN_PROGRESS");
        long review = taskRepository.countByProject_ProjectIdAndStatus(projectId, "REVIEW");
        long done = taskRepository.countByProject_ProjectIdAndStatus(projectId, "DONE");
        long overdue = taskRepository.countOverdue(projectId, LocalDateTime.now());

        BigDecimal percentDone = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(done)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return ProjectDashboardDTO.builder()
                .projectId(projectId)
                .totalTasks(total)
                .openTasks(open)
                .inProgressTasks(inProgress)
                .reviewTasks(review)
                .doneTasks(done)
                .overdueTasks(overdue)
                .percentDone(percentDone)
                .build();
    }

    @Override
    public Project getByIdWithMembers(Long projectId) {
        return projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
    }

    @Override
    public ProjectPerformanceDTO getPerformanceData(Long projectId, String pmEmail) {
    authz.ensurePmOfProject(pmEmail, projectId);

    LocalDate today = LocalDate.now();
    LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
    LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

    List<Object[]> results = taskRepository.countCompletedTasksPerDay(
        projectId,
        startOfWeek.atStartOfDay(),
        endOfWeek.atTime(23, 59, 59)
    );

    Map<String, Long> dayMap = new LinkedHashMap<>();
    results.forEach(r -> dayMap.put((String) r[0], (Long) r[1]));

    List<String> labels = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    List<Long> achieved = new ArrayList<>();
    for (String d : labels) {
        achieved.add(dayMap.getOrDefault(d, 0L));
    }

    long total = taskRepository.countByProject_ProjectId(projectId);
    long targetPerDay = Math.max(1, total / 7);
    List<Long> target = labels.stream().map(d -> targetPerDay).toList();

    return new ProjectPerformanceDTO(labels, achieved, target);

}
   
@Override
public List<ProjectDTO> getTopProjectsByPm(String email, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return projectRepository.findTopProjectsByPm(email, pageable);
}

@Override
public Page<ProjectDTO> getAllProjectsByPm(String email, int page, int size, String keyword) {
    if (keyword == null || keyword.isBlank())
        keyword = "";

    Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
    Page<ProjectDTO> projects = projectRepository.findAllProjectsByPm(email, keyword, pageable);
    System.out.println(projects);
    if (projects == null)
        return Page.empty(pageable);

    return projects.map(dto -> {
        Long projectId = dto.getProjectId();
        if (projectId == null) {
            log.warn("⚠️ ProjectDTO missing projectId for {}", dto.getName());
            return dto;
        }

        List<MemberDTO> allMembers = Optional.ofNullable(
                projectMemberRepository.findMembersByProject(projectId)).orElse(Collections.emptyList());

        int totalMembers = allMembers.size();

        List<MemberDTO> topMembers = allMembers.stream()
                .filter(m -> m.getAvatarUrl() != null)
                .limit(4)
                .toList();

        dto.setMemberCount(totalMembers);
        dto.setMemberAvatars(topMembers.stream().map(MemberDTO::getAvatarUrl).toList());
        dto.setMemberNames(topMembers.stream().map(MemberDTO::getName).toList());

        return dto;
    });
}

    @Override
    public Project enableShareLink(Long projectId, String pmEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        authz.ensurePmOfProject(pmEmail, projectId);
        

        if (!project.isAllowLinkJoin()) {
            String inviteLink = UUID.randomUUID().toString();
            project.setInviteLink(inviteLink);
            project.setAllowLinkJoin(true);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);

            // activityService.log("PROJECT", projectId, "ENABLE_SHARE", "Link: " + inviteLink);
        }

        return project;
    }
    @Override
    public Project disableShareLink(Long projectId, String pmEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        authz.ensurePmOfProject(pmEmail, projectId);

        if (project.isAllowLinkJoin()) {
            project.setAllowLinkJoin(false);
            project.setInviteLink(null);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);

            activityService.log("PROJECT", projectId, "DISABLE_SHARE", "Share link disabled");
        }

        return project;
    }
   
    @Override
    public ProjectMember joinProjectByLink(String inviteLink, Long userId) {
        Project project = projectRepository.findActiveSharedProject(inviteLink)
                .orElseThrow(() -> new BadRequestException("Link mời không hợp lệ hoặc đã bị tắt"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        boolean exists = projectMemberRepository.existsByProject_ProjectIdAndUser_UserId(project.getProjectId(),
                userId);
        if (exists) {
            throw new BadRequestException("Bạn đã là thành viên của dự án này");
        }

        ProjectMember newMember = new ProjectMember();
        newMember.setProject(project);
        newMember.setUser(user);
        newMember.setRoleInProject("Member");
        newMember.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(newMember);

        activityService.log("PROJECT", project.getProjectId(), "JOIN_BY_LINK", user.getEmail());
        notificationService.notifyMemberAdded(project, user);

        return newMember;
    }
    
    public List<Project> getProjectsByUsername(String username) {
        // 1. Tìm User bằng username (email)
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        // 2. Gọi lại phương thức cũ bằng userId
        return this.getProjectsByUser(user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSearchResponseDTO> searchProjectsByKeyword(String keyword) {
        List<Project> projects = projectRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        return projects.stream()
                .map(ProjectSearchResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getUserRoleInProject(Long projectId, Long userId) {
        return projectMemberRepository.findRoleInProject(projectId, userId)
                .orElse("Member"); 
    }

    @Transactional(readOnly = true)
    public String getUserRoleInProjectByEmail(Long projectId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
        return getUserRoleInProject(projectId, user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectFilterDTO> getActiveProjectsForUser(Long userId) {
        return projectRepository.findActiveProjectsByUser(userId);
    }

}

