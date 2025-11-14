package com.devcollab.service.impl.core;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.*;
import com.devcollab.dto.*;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.TaskFollowerService;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.feature.AttachmentService;
import com.devcollab.service.feature.CommentService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;

import org.hibernate.Hibernate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final ProjectRepository projectRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;
    private final ActivityService activityService;
    private final TaskFollowerRepository taskFollowerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
<<<<<<< HEAD
    private final ProjectAuthorizationService projectAuthorizationService;
=======
    private final TaskFollowerService taskFollowerService;
    private final CommentService commentService;
    private final AttachmentService attachmentService;
>>>>>>> payment

    @Override
    public Task createTaskFromDTO(TaskDTO dto, Long creatorId) {
        if (dto == null)
            throw new BadRequestException("Dữ liệu task rỗng");

        Task task = new Task();
        task.setTitle(dto.getTitle());
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        task.setDescriptionMd(dto.getDescriptionMd());
        task.setOrderIndex(0);
        task.setStatus("OPEN");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        if (dto.getDeadline() != null && !dto.getDeadline().isBlank()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline()));
            } catch (Exception e) {
                try {
                    task.setDeadline(LocalDateTime.parse(dto.getDeadline() + "T00:00:00"));
                } catch (Exception ignored) {
                }
            }
        }

        BoardColumn column = boardColumnRepository.findById(dto.getColumnId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cột"));
        Project project = column.getProject();

        task.setColumn(column);
        task.setProject(project);

        if (creatorId != null) {
            User creator = new User();
            creator.setUserId(creatorId);
            task.setCreatedBy(creator);
        } else {
            throw new BadRequestException("Không có thông tin người tạo task");
        }

        Task saved = taskRepository.save(task);

        activityService.log("TASK", saved.getTaskId(), "CREATE_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\",\"column\":\""
                        + escapeJson(column.getName()) + "\"}",
                saved.getCreatedBy());

        return saved;
    }

    @Override
    @Transactional
    public Task quickCreate(String title, Long columnId, Long projectId, Long creatorId) {

        if (title == null || title.isBlank())
            throw new BadRequestException("Tiêu đề task không được để trống");

        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cột"));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        User actor = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người tạo task"));

        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isMember = authz.isMemberOfProject(email, projectId);

        if (!isPm && !isMember) {
            throw new AccessDeniedException(
                    "Chỉ PM/ADMIN hoặc thành viên dự án mới được tạo task.");
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setColumn(column);
        task.setProject(project);
        task.setStatus("OPEN");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        User creator = new User();
        creator.setUserId(creatorId);
        task.setCreatedBy(creator);

        Task saved = taskRepository.save(task);

        activityService.log("TASK", saved.getTaskId(), "CREATE_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\",\"column\":\""
                        + escapeJson(column.getName()) + "\"}",
                actor);

        return saved;
    }

    @Override
    public Task createTask(Task task, Long creatorId) {
        if (task == null)
            throw new BadRequestException("Task rỗng");

        if (task.getCreatedAt() == null)
            task.setCreatedAt(LocalDateTime.now());
        if (task.getUpdatedAt() == null)
            task.setUpdatedAt(LocalDateTime.now());
        if (task.getStatus() == null)
            task.setStatus("OPEN");

        if (creatorId != null) {
            User creator = new User();
            creator.setUserId(creatorId);
            task.setCreatedBy(creator);
        }

        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Long id, Task patch) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        if (patch.getTitle() != null)
            existing.setTitle(patch.getTitle());
        if (patch.getDescriptionMd() != null)
            existing.setDescriptionMd(patch.getDescriptionMd());
        if (patch.getPriority() != null)
            existing.setPriority(patch.getPriority());
        if (patch.getStatus() != null)
            existing.setStatus(patch.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(existing);

        activityService.log("TASK", saved.getTaskId(), "EDIT_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}", saved.getCreatedBy());

        return saved;
    }

    @Override
    @Transactional
    public void deleteTask(Long id, User actor) {

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task không tồn tại"));

        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();

        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isCreator =
                task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(actorId);

        if (!isPm && !isCreator) {
            throw new AccessDeniedException(
                    " Chỉ PM/ADMIN hoặc người tạo task mới có quyền xóa task này.");
        }

        taskRepository.delete(task);

        activityService.log("TASK", id, "DELETE_TASK",
                "{\"title\":\"" + escapeJson(task.getTitle()) + "\"}", actor);

        log.info(" Task '{}' (ID={}) đã bị xóa bởi {}", task.getTitle(), id, actor.getName());
    }

    @Override
    public Task assignTask(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        User assignee = new User();
        assignee.setUserId(assigneeId);
        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "ASSIGN_TASK", "{\"assigneeId\":" + assigneeId + "}",
                task.getCreatedBy());

        return saved;
    }

    @Override
    @Transactional
    public TaskDTO moveTask(Long taskId, MoveTaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        Long projectId = task.getProject().getProjectId();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập!");
        }

        String email;
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User ou) {
            email = String.valueOf(ou.getAttributes().get("email"));
        } else if (principal instanceof String s) {
            email = s;
        } else {
            throw new AccessDeniedException("Không thể xác định người dùng hiện tại!");
        }

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);
        authz.ensurePmOfProject(email, projectId);

        BoardColumn oldCol = task.getColumn();
        BoardColumn newCol = boardColumnRepository.findById(req.getTargetColumnId())
                .orElseThrow(() -> new RuntimeException("Target column not found"));

        task.setColumn(newCol);
        task.setOrderIndex(req.getNewOrderIndex());
        task.setUpdatedAt(LocalDateTime.now());

        String colName = newCol.getName().trim().toLowerCase();

        if (colName.contains("backlog")) {
            task.setStatus("BACKLOG");
            task.setClosedAt(null);
        } else if (colName.contains("to-do") || colName.contains("todo")
                || colName.contains("plan")) {
            task.setStatus("OPEN");
            task.setClosedAt(null);
        } else if (colName.contains("in progress") || colName.contains("doing")
                || colName.contains("working")) {
            task.setStatus("IN_PROGRESS");
            task.setClosedAt(null);
        } else if (colName.contains("review") || colName.contains("verify")
                || colName.contains("qa")) {
            task.setStatus("REVIEW");
            task.setClosedAt(null);
        } else if (colName.contains("done") || colName.contains("completed")
                || colName.contains("finish")) {
            task.setStatus("DONE");
            task.setClosedAt(LocalDateTime.now());
        } else {
            task.setClosedAt(null);
        }

        taskRepository.save(task);

        activityService.log("TASK", taskId, "MOVE_COLUMN",
                String.format("{\"from\":\"%s\",\"to\":\"%s\"}",
                        escapeJson(oldCol != null ? oldCol.getName() : "Unknown"),
                        escapeJson(newCol.getName())),
                task.getCreatedBy());

        return TaskDTO.fromEntity(task);
    }

    @Override
    public Task closeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setStatus("CLOSED");
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "CLOSE_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}", saved.getCreatedBy());
        return saved;
    }

    @Override
    public Task reopenTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setStatus("OPEN");
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "REOPEN_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}", saved.getCreatedBy());
        return saved;
    }

    @Override
    @Transactional
    public TaskDTO updateTaskDescription(Long id, String description) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập!");
        }

        String email;
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User ou) {
            email = String.valueOf(ou.getAttributes().get("email"));
        } else if (principal instanceof String s) {
            email = s;
        } else {
            throw new AccessDeniedException("Không thể xác định người dùng hiện tại!");
        }

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);
        authz.ensurePmOfProject(email, projectId);

        task.setDescriptionMd(description);
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        return TaskDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public TaskDTO updateDates(Long taskId, TaskDTO dto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        Long projectId = task.getProject().getProjectId();

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập!");
        }

        String email;
        Object principal = auth.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User ou) {
            email = String.valueOf(ou.getAttributes().get("email"));
        } else if (principal instanceof String s) {
            email = s;
        } else {
            throw new AccessDeniedException("Không thể xác định người dùng hiện tại!");
        }

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);
        authz.ensurePmOfProject(email, projectId);

        User actor = getCurrentUserOrNull();

        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime oldDeadline = task.getDeadline();
        LocalDateTime newDeadline = null;

        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            try {
                LocalDateTime start = LocalDateTime.parse(dto.getStartDate(), iso);
                task.setStartDate(start);
            } catch (Exception e) {
                log.warn(" Invalid startDate format: {}", dto.getStartDate());
                throw new IllegalArgumentException(" Định dạng startDate không hợp lệ!");
            }
        }

        if (dto.getDeadline() != null && !dto.getDeadline().isBlank()) {
            try {
                newDeadline = LocalDateTime.parse(dto.getDeadline(), iso);
                if (newDeadline.isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException(
                            " Deadline không được nhỏ hơn thời gian hiện tại!");
                }
                task.setDeadline(newDeadline);
            } catch (IllegalArgumentException e) {
                log.warn(" {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.warn(" Invalid deadline format: {}", dto.getDeadline());
                throw new IllegalArgumentException(" Định dạng deadline không hợp lệ!");
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "UPDATE_DATES",
                String.format("{\"start\":\"%s\",\"deadline\":\"%s\"}", dto.getStartDate(),
                        dto.getDeadline()),
                actor);

        if (newDeadline != null && (oldDeadline == null || !newDeadline.equals(oldDeadline))) {
            sendDeadlineNotification(saved, actor);
        }

        log.info("[Deadline Updated] {} chỉnh deadline của task '{}'",
                actor != null ? actor.getName() : "System", task.getTitle());

        return TaskDTO.fromEntity(saved);
    }

    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        String email = null;

        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        } else if (auth
                .getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User ou) {
            Object em = ou.getAttributes().get("email");
            if (em != null)
                email = String.valueOf(em);
        }

        if (email == null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof String s) {
                email = s;
            }
        }

        if (email == null || email.isBlank())
            return null;

        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public List<TaskDTO> getTasksByProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        return taskRepository.findByProject_ProjectIdAndArchivedFalse(projectId).stream()
                .map(TaskDTO::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<Task> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssignee_UserId(userId);
    }

    @Override
    public List<Task> getTasksByProjectAndMember(Long projectId, String email) {
        throw new UnsupportedOperationException("Chưa triển khai: getTasksByProjectAndMember");
    }

    @Override
    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
    }

    @Override
    public TaskDTO getByIdAsDTO(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        return TaskDTO.fromEntity(task);
    }

    private String escapeJson(String text) {
        return text == null ? ""
                : text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    @Override
    @Transactional
    public boolean archiveTask(Long taskId, User actor) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();
        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isCreator =
                task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(actorId);

        if (!isPm && !isCreator) {
            throw new AccessDeniedException(
                    " Chỉ PM/ADMIN hoặc người tạo task mới được archive task này.");
        }
        task.setArchived(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return true;
    }

    @Override
    @Transactional
    public boolean restoreTask(Long taskId, User actor) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();
        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean allowedAsPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            allowedAsPm = true;
        } catch (Exception ignored) {
        }

        boolean allowedAsCreator =
                task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(actorId);

        if (!allowedAsPm && !allowedAsCreator) {
            throw new AccessDeniedException("Bạn không có quyền restore task này.");
        }

        task.setArchived(false);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        return true;
    }

    @Override
    @Transactional
    public TaskDTO markComplete(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();

        // 1️⃣ Kiểm tra PM / ADMIN của dự án
        boolean isPmOrAdmin = false;
        try {
            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại"));
            projectAuthorizationService.ensurePmOfProject(currentUser.getEmail(), projectId);
            isPmOrAdmin = true; // nếu không exception => là PM/ADMIN
        } catch (Exception ignored) {
        }

        // 2️⃣ Kiểm tra follower
        boolean isFollower =
                taskFollowerRepository.existsByTask_TaskIdAndUser_UserId(taskId, userId);

        // 3️⃣ Kiểm tra creator
        boolean isCreator =
                task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(userId);

        // 4️⃣ Kiểm tra assignee
        boolean isAssignee =
                task.getAssignee() != null && task.getAssignee().getUserId().equals(userId);

        // 🔥 Quyền hợp lệ = PM/ADMIN OR Assignee OR Follower OR Creator
        if (!(isPmOrAdmin || isAssignee || isFollower || isCreator)) {
            throw new SecurityException("Bạn không có quyền đánh dấu hoàn thành task này");
        }

        if ("DONE".equalsIgnoreCase(task.getStatus())) {
            return TaskDTO.fromEntity(task);
        }

        task.setStatus("DONE");
        task.setClosedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        Hibernate.initialize(saved.getAssignee());
        Hibernate.initialize(saved.getCreatedBy());
        Hibernate.initialize(saved.getProject());
        Hibernate.initialize(saved.getColumn());
        Hibernate.initialize(saved.getLabels());

        activityService.log("TASK", taskId, "MARK_COMPLETE",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}", saved.getCreatedBy());

        log.info("[TASK DONE] Task '{}' (ID={}) đã được đánh dấu hoàn thành bởi user {}",
                saved.getTitle(), taskId, userId);

        return TaskDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public TaskDTO markIncomplete(Long taskId, Long userId, String email) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task không tồn tại"));

        Long projectId = task.getProject().getProjectId();

        // 1️⃣ Kiểm tra PM / ADMIN của dự án (xài hàm có sẵn)
        boolean isPmOrAdmin = false;
        try {
            projectAuthorizationService.ensurePmOfProject(email, projectId);
            isPmOrAdmin = true; // nếu không exception => là PM/ADMIN
        } catch (Exception ignored) {
        }

        // 2️⃣ Kiểm tra assignee
        boolean isAssignee =
                task.getAssignee() != null && task.getAssignee().getUserId().equals(userId);

        // 3️⃣ Kiểm tra follower
        boolean isFollower =
                taskFollowerRepository.existsByTask_TaskIdAndUser_UserId(taskId, userId);

        // 4️⃣ Kiểm tra creator
        boolean isCreator =
                task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(userId);

        // 🔥 Quyền hợp lệ = PM/ADMIN OR Assignee OR Follower OR Creator
        if (!(isPmOrAdmin || isAssignee || isFollower || isCreator)) {
            throw new AccessDeniedException("Bạn không có quyền mở lại task này.");
        }

        // Nếu không phải DONE → không làm gì
        if (!"DONE".equalsIgnoreCase(task.getStatus())) {
            return TaskDTO.fromEntity(task);
        }

        // Cập nhật lại trạng thái
        task.setStatus("OPEN");
        task.setClosedAt(null);
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        return TaskDTO.fromEntity(saved);
    }

    private void sendDeadlineNotification(Task task, User actor) {
        try {
            if (task == null || task.getDeadline() == null)
                return;

            String link =
                    "/projects/" + task.getProject().getProjectId() + "/tasks/" + task.getTaskId();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            String deadlineStr = task.getDeadline().format(fmt);

            String title = "Công việc sắp đến hạn";
            String message =
                    "Công việc \"" + task.getTitle() + "\" sắp đến hạn vào: " + deadlineStr;

            List<User> receivers = new ArrayList<>();
            if (task.getAssignee() != null)
                receivers.add(task.getAssignee());
            if (task.getCreatedBy() != null)
                receivers.add(task.getCreatedBy());
            if (task.getFollowers() != null && !task.getFollowers().isEmpty()) {
                task.getFollowers().forEach(f -> {
                    if (f.getUser() != null)
                        receivers.add(f.getUser());
                });
            }
            List<User> filtered =
                    receivers.stream().filter(Objects::nonNull).filter(u -> u.getUserId() != null)
                            .filter(u -> actor == null || !u.getUserId().equals(actor.getUserId()))
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toMap(User::getUserId, u -> u, (a, b) -> a),
                                    m -> new ArrayList<>(m.values())));

            if (filtered.isEmpty()) {
                log.debug("ℹ Không có người nhận thông báo deadline cho task '{}'",
                        task.getTitle());
                return;
            }

            for (User receiver : filtered) {
                notificationService.createNotification(receiver, "TASK_DUE_SOON", task.getTaskId(),
                        title, message, link, actor);
            }

            log.info("[Deadline] Đã gửi 'TASK_DUE_SOON' cho {} người trong task '{}'",
                    filtered.size(), task.getTitle());

        } catch (Exception e) {
            log.error(" sendDeadlineNotification() failed: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void removeDeadline(Long taskId, User actor) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        try {
            authz.ensurePmOfProject(actor.getEmail(), projectId);
        } catch (Exception e) {
            throw new AccessDeniedException("Chỉ PM/ADMIN mới được xóa deadline của task.");
        }

        task.setDeadline(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        activityService.log("TASK", taskId, "REMOVE_DEADLINE", "{\"message\":\"Deadline removed\"}",
                actor);
    }

    @Override
    public List<Task> getTasksByAssignee(User user) {
        return taskRepository.findTasksByAssignee(user);
    }

    @Override
    public List<Task> getTasksCreatedBy(User user) {
        return taskRepository.findTasksCreatedBy(user);
    }

    @Override
    public Map<String, Object> getPercentDoneByStatus(Long projectId) {
        List<Map<String, Object>> raw = taskRepository.countTasksByStatus(projectId);

        long total = raw.stream().mapToLong(m -> ((Number) m.get("count")).longValue()).sum();

        Map<String, Double> temp = new HashMap<>();
        raw.forEach(m -> {
            String status = (String) m.get("status");
            long count = ((Number) m.get("count")).longValue();
            double percent = total == 0 ? 0 : ((double) count / total) * 100;
            temp.put(status, percent);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("DONE", String.format("%.1f", temp.getOrDefault("DONE", 0.0)));
        result.put("IN_PROGRESS", String.format("%.1f", temp.getOrDefault("IN_PROGRESS", 0.0)));
        result.put("OPEN", String.format("%.1f", temp.getOrDefault("OPEN", 0.0)));
        result.put("total", total);

        return result;
    }

    @Override
    public List<MemberPerformanceDTO> getMemberPerformance(Long projectId) {
        List<Object[]> rows = taskRepository.findMemberPerformanceByProject(projectId);
        List<MemberPerformanceDTO> list = new ArrayList<>();

        for (Object[] r : rows) {
            MemberPerformanceDTO dto = new MemberPerformanceDTO();
            dto.setUserId(((Number) r[0]).longValue());
            dto.setName((String) r[1]);
            dto.setEmail((String) r[2]);
            dto.setTotalTasks(((Number) r[3]).longValue());
            dto.setCompletedTasks(((Number) r[4]).longValue());
            dto.setOnTimeTasks(((Number) r[5]).longValue());
            dto.setLateTasks(((Number) r[6]).longValue());
            dto.setAvgDelayHours(((Number) r[7]).doubleValue());
            dto.setPriorityPoints(((Number) r[8]).intValue());

            double score = 0;
            if (dto.getTotalTasks() > 0) {
                double completion = ((double) dto.getCompletedTasks() / dto.getTotalTasks()) * 50;
                double punctuality = dto.getCompletedTasks() > 0
                        ? ((double) dto.getOnTimeTasks() / dto.getCompletedTasks()) * 30
                        : 0;
                double effort = ((double) dto.getPriorityPoints() / dto.getTotalTasks()) * 20;
                score = completion + punctuality + effort;
                if (score > 100)
                    score = 100;
            }

            dto.setPerformanceScore(Math.round(score * 100.0) / 100.0);
            list.add(dto);
        }

        return list;
    }

    @Override
    public List<Task> getTasksByUser(User user) {
        return taskRepository.findAllUserTasks(user);
    }

    @Override
<<<<<<< HEAD
    public Page<Task> getUserTasksPaged(User user, String sortBy, int page, int size,
            String status) {
=======
    @Transactional(readOnly = true)
    public Page<Task> getUserTasksPaged(User user, String sortBy, int page, int size, String status) {
>>>>>>> payment
        Pageable pageable = PageRequest.of(page, size);

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return taskRepository.findUserTasksByStatus(user, status.toUpperCase(), pageable);
        }

        String key = (sortBy == null || sortBy.isBlank()) ? "deadline" : sortBy.toLowerCase();
        return switch (key) {
            case "priority" -> taskRepository.findUserTasksOrderByPriority(user, pageable);
            case "project" -> taskRepository.findUserTasksOrderByProject(user, pageable);
            case "deadline" -> taskRepository.findUserTasksOrderByDeadline(user, pageable);
            default -> taskRepository.findUserTasksOrderByDeadline(user, pageable);
        };
    }

    @Override
    public List<Task> findUpcomingDeadlines(Long userId) {
        return taskRepository.findTopUpcoming(userId, PageRequest.of(0, 5));
    }
    @Override
    public TaskStatisticsDTO getTaskStatistics(User user) {
        List<Task> userTasks = taskRepository.findAllUserTasks(user);

        // Count tasks by status
        Map<String, Long> statusCount = userTasks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        task -> task.getStatus() != null ? task.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));

        long totalTasks = userTasks.size();

        // Calculate percentages
        Map<String, Double> statusPercentages = new HashMap<>();
        if (totalTasks > 0) {
            statusCount.forEach((status, count) -> {
                double percentage = (count.doubleValue() / totalTasks) * 100;
                statusPercentages.put(status, Math.round(percentage * 100.0) / 100.0);
            });
        }

        // Ensure we have all required statuses (even if count is 0)
        ensureRequiredStatuses(statusCount, statusPercentages, totalTasks);

        return new TaskStatisticsDTO(statusCount, statusPercentages, totalTasks);
    }

    private void ensureRequiredStatuses(Map<String, Long> statusCount,
                                        Map<String, Double> statusPercentages,
                                        long totalTasks) {
        String[] requiredStatuses = {"OPEN", "IN_PROGRESS", "DONE"};

        for (String status : requiredStatuses) {
            if (!statusCount.containsKey(status)) {
                statusCount.put(status, 0L);
                statusPercentages.put(status, 0.0);
            }
        }
    }
    // In your TaskService

    @Override
    @Transactional(readOnly = true)
    public Page<TaskReviewDTO> getTasksForReviewPaged(Long projectId, int page, int size, String status, String search) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> taskPage;

        // FILTER BY STATUS + SEARCH
        if (status != null && !status.isBlank() && search != null && !search.isBlank()) {
            taskPage = taskRepository
                    .findByProject_ProjectIdAndStatusAndTitleContainingIgnoreCase(projectId, status, search, pageable);

        } else if (status != null && !status.isBlank()) {
            taskPage = taskRepository
                    .findByProject_ProjectIdAndStatus(projectId, status, pageable);

        } else if (search != null && !search.isBlank()) {
            taskPage = taskRepository
                    .findByProject_ProjectIdAndTitleContainingIgnoreCase(projectId, search, pageable);

        } else {
            taskPage = taskRepository.findByProject_ProjectId(projectId, pageable);
        }

        return taskPage.map(task -> {
            List<TaskFollowerDTO> followers = taskFollowerService.getFollowersByTask(task.getTaskId());
            return TaskReviewDTO.fromEntity(task, followers);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailDTO getTaskDetailForReview(Long taskId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        // Fully initialize lazy fields INSIDE transaction
        Hibernate.initialize(task.getAssignee());
        Hibernate.initialize(task.getCreatedBy());
        Hibernate.initialize(task.getProject());
        Hibernate.initialize(task.getColumn());
        Hibernate.initialize(task.getLabels());
        Hibernate.initialize(task.getFollowers());

        // Load DTO data
        List<TaskFollowerDTO> followers = taskFollowerService.getFollowersByTask(taskId);
        List<CommentDTO> comments = commentService.getCommentsByTask(taskId);
        List<AttachmentDTO> attachments = attachmentService.getAttachmentDTOsByTask(taskId);

        return TaskDetailDTO.fromEntity(task, followers, comments, attachments);
    }


}
