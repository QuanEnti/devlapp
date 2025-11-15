package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.ProjectSummaryDTO;
import com.devcollab.dto.response.ProjectDashboardDTO;
import com.devcollab.dto.response.ProjectPerformanceDTO;
import com.devcollab.dto.response.ProjectSearchResponseDTO;
import com.devcollab.dto.userTaskDto.ProjectFilterDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.JoinRequestService;
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
    private final RoleRepository roleRepository;
    private final AppEventService appEventService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final JoinRequestService joinRequestService;

    @Override
    public Project createProject(Project project, Long creatorId) {
        if (project == null || project.getName() == null || project.getName().isBlank()) {
            throw new BadRequestException("Tên dự án không được để trống");
        }
        String projectName = project.getName().trim();

        boolean exists =
                projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId(projectName, creatorId);
        if (exists) {
            throw new BadRequestException("Tên dự án bị trùng");
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
        System.out.println("CvIMG" + project.getCoverImage());
        project.setCoverImage(project.getCoverImage());

        if (project.getStatus() == null)
            project.setStatus("Active");
        if (project.getPriority() == null)
            project.setPriority("Normal");
        if (project.getVisibility() == null)
            project.setVisibility("private");


        Project saved = projectRepository.save(project);

        String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        saved.setInviteLink(inviteCode);
        saved.setInviteCreatedAt(LocalDateTime.now());
        saved.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
        saved.setInviteUsageCount(0);
        saved.setInviteMaxUses(10);
        saved.setInviteCreatedBy(creator.getEmail());
        saved.setAllowLinkJoin(false);
        saved = projectRepository.save(saved);

        ProjectMember pm = new ProjectMember();
        pm.setProject(saved);
        pm.setUser(creator);
        pm.setRoleInProject("PM");
        pm.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(pm);

        Role pmRole = roleRepository.findByName("ROLE_PM")
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ROLE_PM trong hệ thống"));
        boolean hasPmRole =
                creator.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_PM"));
        if (!hasPmRole) {
            creator.getRoles().add(pmRole);
            userRepository.save(creator);
        }

        String[] defaultCols = {"Backlog", "To-do", "In Progress", "Review", "Done"};
        String[] defaultStatusCodes = {"BACKLOG", "OPEN", "IN_PROGRESS", "REVIEW", "DONE"};

        for (int i = 0; i < defaultCols.length; i++) {
            BoardColumn col = new BoardColumn();
            col.setProject(saved);
            col.setName(defaultCols[i]);
            col.setOrderIndex(i + 1);
            col.setIsDefault(true);
            try {
                BoardColumn.class.getDeclaredField("statusCode");
                col.getClass().getMethod("setStatusCode", String.class).invoke(col,
                        defaultStatusCodes[i]);
            } catch (Exception ignored) {
            }
            boardColumnRepository.save(col);
        }
        appEventService.publishProjectCreated(saved);

        return saved;
    }


    @Override
    public Project updateProject(Long id, Project patch) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
        // ✅ Kiểm tra tên trùng (nếu có cập nhật)
        if (patch.getName() != null && !patch.getName().isBlank()) {
            String projectName = patch.getName().trim();
            boolean exists =
                    projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserIdAndProjectIdNot(
                            projectName, existing.getCreatedBy().getUserId(), id);
            if (exists) {
                throw new BadRequestException("Tên dự án bị trùng");
            }
            existing.setName(projectName);
        }
        // ✅ Cập nhật mô tả
        if (patch.getDescription() != null)
            existing.setDescription(patch.getDescription());
        // ✅ Cập nhật Business Rule
        if (patch.getBusinessRule() != null)
            existing.setBusinessRule(patch.getBusinessRule());
        // ✅ Cập nhật độ ưu tiên
        if (patch.getPriority() != null)
            existing.setPriority(patch.getPriority());
        // ✅ Cập nhật status
        if (patch.getStatus() != null && !patch.getStatus().isEmpty())
            existing.setStatus(patch.getStatus());
        if (patch.getVisibility() != null)
            existing.setVisibility(patch.getVisibility());
        if (patch.getStartDate() != null)
            existing.setStartDate(patch.getStartDate());
        // ✅ Cập nhật ngày kết thúc (và kiểm tra hợp lệ)
        if (patch.getDueDate() != null) {
            if (existing.getStartDate() != null
                    && patch.getDueDate().isBefore(existing.getStartDate())) {
                throw new BadRequestException("Ngày kết thúc không hợp lệ");
            }
            existing.setDueDate(patch.getDueDate());
        }
        existing.setCoverImage(patch.getCoverImage());
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

        boolean exists = projectMemberRepository.findByProject_ProjectId(projectId).stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (exists) {
            return projectMemberRepository.findByProject_ProjectId(projectId).stream()
                    .filter(m -> m.getUser().getUserId().equals(userId)).findFirst().get();
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
        ProjectMember target = members.stream().filter(m -> m.getUser().getUserId().equals(userId))
                .findFirst().orElseThrow(() -> new NotFoundException("Thành viên không tồn tại"));

        long pmCount =
                members.stream().filter(m -> "PM".equalsIgnoreCase(m.getRoleInProject())).count();
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
        long inProgress =
                taskRepository.countByProject_ProjectIdAndStatus(projectId, "IN_PROGRESS");
        long review = taskRepository.countByProject_ProjectIdAndStatus(projectId, "REVIEW");
        long done = taskRepository.countByProject_ProjectIdAndStatus(projectId, "DONE");
        long overdue = taskRepository.countOverdue(projectId, LocalDateTime.now());

        BigDecimal percentDone = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(done).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return ProjectDashboardDTO.builder().projectId(projectId).totalTasks(total).openTasks(open)
                .inProgressTasks(inProgress).reviewTasks(review).doneTasks(done)
                .overdueTasks(overdue).percentDone(percentDone)
                .projectStatus(project.getStatus() != null ? project.getStatus() : "Active")
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

        List<Object[]> results = taskRepository.countCompletedTasksPerDay(projectId,
                startOfWeek.atStartOfDay(), endOfWeek.atTime(23, 59, 59));

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

        if (projects == null)
            return Page.empty(pageable);

        return projects.map(dto -> {
            Long projectId = dto.getProjectId();
            if (projectId == null) {
                log.warn("⚠️ ProjectDTO missing projectId for {}", dto.getName());
                return dto;
            }

            List<MemberDTO> allMembers =
                    Optional.ofNullable(projectMemberRepository.findMembersByProject(projectId))
                            .orElse(Collections.emptyList());

            int totalMembers = allMembers.size();

            List<MemberDTO> topMembers =
                    allMembers.stream().filter(m -> m.getAvatarUrl() != null).limit(4).toList();

            dto.setMemberCount(totalMembers);
            dto.setMemberAvatars(topMembers.stream().map(MemberDTO::getAvatarUrl).toList());
            dto.setMemberNames(topMembers.stream().map(MemberDTO::getName).toList());

            return dto;
        });
    }

    @Override
    public Project enableShareLink(Long projectId, String creatorEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        authz.ensurePmOfProject(creatorEmail, projectId);

        project.setInviteCreatedBy(creatorEmail);

        boolean expired = project.getInviteExpiredAt() != null
                && project.getInviteExpiredAt().isBefore(LocalDateTime.now());
        boolean limitReached = project.getInviteUsageCount() >= project.getInviteMaxUses();

        if (project.getInviteLink() == null || expired || limitReached) {
            String newCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            project.setInviteLink(newCode);
            project.setInviteCreatedAt(LocalDateTime.now());
            project.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
            project.setInviteUsageCount(0);
            project.setInviteMaxUses(10);
        }

        project.setAllowLinkJoin(true);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        return project;
    }


    @Override
    public Project disableShareLink(Long projectId, String pmEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        authz.ensurePmOfProject(pmEmail, projectId);

        if (project.isAllowLinkJoin()) {
            project.setAllowLinkJoin(false);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);

            activityService.log("PROJECT", projectId, "DISABLE_SHARE",
                    "Share link disabled (link preserved)");
        }
        return project;
    }

    @Override
    @Transactional
    public Map<String, Object> joinProjectByLink(String inviteLink, Long userId) {
        Project project = projectRepository.findByInviteLink(inviteLink)
                .orElseThrow(() -> new BadRequestException("Liên kết mời không hợp lệ!"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        if (!project.isAllowLinkJoin()) {
            throw new BadRequestException("Liên kết mời đã bị vô hiệu hóa!");
        }

        boolean expired = project.getInviteExpiredAt() != null
                && project.getInviteExpiredAt().isBefore(LocalDateTime.now());
        boolean limitReached = project.getInviteUsageCount() >= project.getInviteMaxUses();

        if ((expired || limitReached) && project.isInviteAutoRegen()) {
            String newCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            project.setInviteLink(newCode);
            project.setInviteCreatedAt(LocalDateTime.now());
            project.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
            project.setInviteUsageCount(0);
            project.setUpdatedAt(LocalDateTime.now());
            projectRepository.save(project);

            activityService.log("PROJECT", project.getProjectId(), "AUTO_REGEN_LINK",
                    "Link cũ hết hạn hoặc đầy, hệ thống đã tự tạo link mới: " + newCode);
            notificationService.notifyProjectLinkRegenerated(project);

            throw new BadRequestException(
                    "Liên kết mời đã hết hạn. Hệ thống đã tạo liên kết mới, vui lòng yêu cầu PM gửi lại.");
        }

        if (expired)
            throw new BadRequestException("Liên kết mời đã hết hạn!");
        if (limitReached)
            throw new BadRequestException(
                    "Liên kết này đã đạt giới hạn mời (" + project.getInviteMaxUses() + " người)!");

        boolean exists = projectMemberRepository
                .existsByProject_ProjectIdAndUser_UserId(project.getProjectId(), userId);
        if (exists)
            throw new BadRequestException("Bạn đã là thành viên của dự án này!");

        String creatorEmail = project.getInviteCreatedBy();

        boolean creatorIsPm = false;
        if (creatorEmail != null && !creatorEmail.trim().isEmpty()) {
            creatorIsPm = projectMemberRepository
                    .existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(
                            project.getProjectId(), creatorEmail, List.of("PM", "ADMIN", "OWNER"));
        }

        // Nếu người copy link không phải PM/ADMIN hoặc null → tạo join request
        if (!creatorIsPm) {
            joinRequestService.createJoinRequest(project, user);
            notificationService.notifyJoinRequestToPM(project, user);

            return Map.of("message", "join_request_sent", "projectId", project.getProjectId(),
                    "projectName", project.getName());
        }
        ProjectMember newMember = new ProjectMember();
        newMember.setProject(project);
        newMember.setUser(user);
        newMember.setRoleInProject("Member");
        newMember.setJoinedAt(LocalDateTime.now());
        projectMemberRepository.save(newMember);

        project.setInviteUsageCount(project.getInviteUsageCount() + 1);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        activityService.log("PROJECT", project.getProjectId(), "JOIN_BY_LINK",
                user.getEmail() + " đã tham gia dự án qua link mời");
        notificationService.notifyMemberAdded(project, user);

        return Map.of("message", "joined_success", "projectId", project.getProjectId(),
                "projectName", project.getName());
    }



    public List<Project> getProjectsByUsername(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
        return this.getProjectsByUser(user.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSearchResponseDTO> searchProjectsByKeyword(String keyword) {
        List<Project> projects = projectRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
        return projects.stream().map(ProjectSearchResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public String getUserRoleInProject(Long projectId, Long userId) {
        return projectMemberRepository.findRoleInProject(projectId, userId).orElse("Member");
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

    @Override
    public Map<String, Object> getProgress(Long projectId) {
        try {
            return projectRepository.getProjectProgress(projectId);
        } catch (Exception e) {
            System.err.println(
                    "Error fetching progress for project " + projectId + ": " + e.getMessage());
            return Map.of("percent_done_by_status", 0, "percent_done_by_checklist", 0);
        }
    }

    @Override
    public Map<String, Object> getMetrics(Long projectId) {
        try {
            return projectRepository.getProjectMetrics(projectId);
        } catch (Exception e) {
            System.err.println(
                    "Error fetching metrics for project " + projectId + ": " + e.getMessage());
            return Map.of("totalTasks", 0, "completedTasks", 0, "overdueTasks", 0, "activeTasks",
                    0);
        }
    }

    @Override
    public Page<ProjectMember> getProjectsByUserSorted(User user, String role, Pageable pageable) {
        if ("manager".equalsIgnoreCase(role)) {
            return projectMemberRepository.findByUserSortedByManager(user, pageable);
        } else if ("member".equalsIgnoreCase(role)) {
            return projectMemberRepository.findByUserSortedByMember(user, pageable);
        } else {
            return projectMemberRepository.findByUser(user, pageable);
        }
    }

    @Override
    public Page<ProjectSummaryDTO> getProjectsByUserPaginated(String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return projectRepository.findByUserEmail(email, pageable)
                .map(p -> new ProjectSummaryDTO(p.getProjectId(), p.getName(), p.getDescription(),
                        p.getStartDate(), p.getDueDate(), p.getStatus(), p.getPriority()));
    }

    @Override
    public boolean existsByNameAndCreatedBy_UserId(String name, Long createdById) {
        if (name == null || createdById == null) {
            return false;
        }
        return projectRepository.existsByNameIgnoreCaseAndCreatedBy_UserId(name.trim(),
                createdById);
    }

    @Override
    public long countAll() {
        return projectRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return projectRepository.countByStatus(status);
    }

    @Override
    public List<Project> getTop5ProjectsByUser(Long userId) {
        try {
            Pageable pageable = PageRequest.of(0, 5);
            return projectRepository.findTopProjectsByUser(userId, pageable);
        } catch (Exception e) {
            log.error("Error fetching top 5 projects for user {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

