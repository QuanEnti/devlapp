package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberPerformanceDTO;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.userTaskDto.TaskCardDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.hibernate.Hibernate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.service.core.TaskFollowerService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    // ----------------------------------------------------
    // ✅ 1. Tạo Task từ DTO
    // ----------------------------------------------------
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
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\",\"column\":\"" + escapeJson(column.getName())
                        + "\"}",
                saved.getCreatedBy());

        return saved;
    }

    // ----------------------------------------------------
    // ✅ 2. Tạo nhanh Task
    // ----------------------------------------------------
    @Override
    public Task quickCreate(String title, Long columnId, Long projectId, Long creatorId) {
        if (title == null || title.isBlank())
            throw new BadRequestException("Tiêu đề task không được để trống");

        BoardColumn column = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cột"));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        Task task = new Task();
        task.setTitle(title.trim());
        task.setColumn(column);
        task.setProject(project);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        if (creatorId != null) {
            User creator = new User();
            creator.setUserId(creatorId);
            task.setCreatedBy(creator);
        }

        Task saved = taskRepository.save(task);

        activityService.log("TASK", saved.getTaskId(), "CREATE_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\",\"column\":\"" + escapeJson(column.getName())
                        + "\"}",
                saved.getCreatedBy());

        return saved;
    }

    // ----------------------------------------------------
    // ✅ 3. Tạo Task thủ công
    // ----------------------------------------------------
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

    // ----------------------------------------------------
    // ✅ 4. Cập nhật Task
    // ----------------------------------------------------
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
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}",
                saved.getCreatedBy());

        return saved;
    }

    // ----------------------------------------------------
    // ✅ 5. Xóa Task
    // ----------------------------------------------------
    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task không tồn tại"));
        taskRepository.deleteById(id);

        activityService.log("TASK", id, "DELETE_TASK",
                "{\"title\":\"" + escapeJson(task.getTitle()) + "\"}",
                task.getCreatedBy());
    }

    // ----------------------------------------------------
    // ✅ 6. Gán người phụ trách
    // ----------------------------------------------------
    @Override
    public Task assignTask(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        User assignee = new User();
        assignee.setUserId(assigneeId);
        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "ASSIGN_TASK",
                "{\"assigneeId\":" + assigneeId + "}", task.getCreatedBy());

        return saved;
    }

    // ----------------------------------------------------
    // ✅ 8. Di chuyển Task (with order)
    // ----------------------------------------------------
    @Override
    @Transactional
    public TaskDTO moveTask(Long taskId, MoveTaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        BoardColumn oldCol = task.getColumn();
        BoardColumn newCol = boardColumnRepository.findById(req.getTargetColumnId())
                .orElseThrow(() -> new RuntimeException("Target column not found"));

        task.setColumn(newCol);
        task.setOrderIndex(req.getNewOrderIndex());
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);

        activityService.log("TASK", taskId, "MOVE_COLUMN",
                String.format("{\"from\":\"%s\",\"to\":\"%s\"}",
                        escapeJson(oldCol != null ? oldCol.getName() : "Unknown"),
                        escapeJson(newCol.getName())),
                task.getCreatedBy());

        return TaskDTO.fromEntity(task);
    }

    // ----------------------------------------------------
    // ✅ 9. Đóng / mở lại Task
    // ----------------------------------------------------
    @Override
    public Task closeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setStatus("CLOSED");
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "CLOSE_TASK",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}",
                saved.getCreatedBy());
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
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}",
                saved.getCreatedBy());
        return saved;
    }

    // ----------------------------------------------------
    // ✅ 10. Cập nhật mô tả Task
    // ----------------------------------------------------
    @Override
    @Transactional
    public TaskDTO updateTaskDescription(Long id, String description) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setDescriptionMd(description);
        task.setUpdatedAt(LocalDateTime.now());

        Task saved = taskRepository.save(task);

        // ✅ Chuyển sang DTO ngay trong Transaction (Session vẫn còn mở)
        return TaskDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public TaskDTO updateDates(Long taskId, TaskDTO dto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("❌ Task not found with ID: " + taskId));

        // 👤 Lấy actor hiện tại (user đang thao tác)
        User actor = getCurrentUserOrNull();

        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime oldDeadline = task.getDeadline();
        LocalDateTime newDeadline = null;

        // 🕒 Cập nhật Start Date
        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            try {
                LocalDateTime start = LocalDateTime.parse(dto.getStartDate(), iso);
                task.setStartDate(start);
            } catch (Exception e) {
                log.warn("⚠️ Invalid startDate format: {}", dto.getStartDate());
            }
        }

        // ⏰ Cập nhật Deadline (và chặn quá khứ)
        if (dto.getDeadline() != null && !dto.getDeadline().isBlank()) {
            try {
                newDeadline = LocalDateTime.parse(dto.getDeadline(), iso);
                if (newDeadline.isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("🚫 Deadline không được nhỏ hơn thời gian hiện tại!");
                }
                task.setDeadline(newDeadline);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                log.warn("⚠️ Invalid deadline format: {}", dto.getDeadline());
                throw new IllegalArgumentException("⚠️ Định dạng deadline không hợp lệ!");
            }
        }

        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // 📝 Ghi activity: actor phải là người đang thao tác
        activityService.log(
                "TASK",
                taskId,
                "UPDATE_DATES",
                "{\"start\":\"" + dto.getStartDate() + "\",\"deadline\":\"" + dto.getDeadline() + "\"}",
                actor /* ✅ đúng người thao tác */
        );

        // 🔔 Nếu deadline thay đổi → gửi noti, sender = actor
        if (newDeadline != null && (oldDeadline == null || !newDeadline.equals(oldDeadline))) {
            sendDeadlineNotification(saved, actor /* ✅ đúng người thao tác */);
        }

        log.info("🕓 [Deadline Updated] {} chỉnh deadline của task '{}'",
                actor != null ? actor.getName() : "System", task.getTitle());

        return TaskDTO.fromEntity(saved);
    }

    /** Hỗ trợ cả UsernamePassword & OAuth2/OIDC */
    private User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        String email = null;

        // OIDC / OAuth2
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        } else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User ou) {
            Object em = ou.getAttributes().get("email");
            if (em != null)
                email = String.valueOf(em);
        }

        // Form login / UserDetails
        if (email == null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof String s) { // đôi khi là email/username
                email = s;
            }
        }

        if (email == null || email.isBlank())
            return null;

        return userRepository.findByEmail(email).orElse(null);
    }

    // ----------------------------------------------------
    // ✅ 12. Các truy vấn cơ bản
    // ----------------------------------------------------
    @Override
    public List<TaskDTO> getTasksByProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

        // ⚡️ Chỉ lấy task chưa archived
        return taskRepository.findByProject_ProjectIdAndArchivedFalse(projectId)
                .stream()
                .map(TaskDTO::fromEntity)
                .collect(Collectors.toList());
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

    // ----------------------------------------------------
    // 🧩 Helper
    // ----------------------------------------------------
    private String escapeJson(String text) {
        return text == null ? "" : text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
    
    @Override
    @Transactional
    public boolean archiveTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null)
            return false;

        task.setArchived(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
        return true;
    }
    
    @Override
    @Transactional
    public boolean restoreTask(Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null)
            return false;
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

        boolean allowed = taskFollowerRepository.existsByTask_TaskIdAndUser_UserId(taskId, userId)
                || (task.getCreatedBy() != null && task.getCreatedBy().getUserId().equals(userId));

        if (!allowed)
            throw new SecurityException("⚠️ Chỉ thành viên của task mới có thể đánh dấu hoàn thành");

        if ("DONE".equalsIgnoreCase(task.getStatus()))
            return TaskDTO.fromEntity(task); // đã DONE rồi thì trả về luôn DTO

        task.setStatus("DONE");
        task.setClosedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        // ✅ Khởi tạo các quan hệ cần thiết cho DTO
        Hibernate.initialize(saved.getAssignee());
        Hibernate.initialize(saved.getCreatedBy());
        Hibernate.initialize(saved.getProject());
        Hibernate.initialize(saved.getColumn());
        Hibernate.initialize(saved.getLabels());

        // 🪶 Ghi log hoạt động
        activityService.log("TASK", taskId, "MARK_COMPLETE",
                "{\"title\":\"" + escapeJson(saved.getTitle()) + "\"}",
                saved.getCreatedBy());

        // ✅ Trả về DTO
        return TaskDTO.fromEntity(saved);
    }

  
  private void sendDeadlineNotification(Task task, User actor) {
    try {
        if (task == null || task.getDeadline() == null) return;

        String link = "/projects/" + task.getProject().getProjectId()
                + "/tasks/" + task.getTaskId();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
        String deadlineStr = task.getDeadline().format(fmt);

        String title = "Công việc sắp đến hạn";
        String message = "Công việc \"" + task.getTitle() + "\" sắp đến hạn vào: " + deadlineStr;

        List<User> receivers = new ArrayList<>();
        if (task.getAssignee() != null) receivers.add(task.getAssignee());
        if (task.getCreatedBy() != null) receivers.add(task.getCreatedBy());
        if (task.getFollowers() != null && !task.getFollowers().isEmpty()) {
            task.getFollowers().forEach(f -> { if (f.getUser() != null) receivers.add(f.getUser()); });
        }

        // ✅ Lọc trùng + loại actor
        List<User> filtered = receivers.stream()
                .filter(Objects::nonNull)
                .filter(u -> u.getUserId() != null)
                .filter(u -> actor == null || !u.getUserId().equals(actor.getUserId()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getUserId, u -> u, (a,b)->a),
                        m -> new ArrayList<>(m.values())
                ));

        if (filtered.isEmpty()) {
            log.debug("ℹ️ Không có người nhận thông báo deadline cho task '{}'", task.getTitle());
            return;
        }

        for (User receiver : filtered) {
            notificationService.createNotification(
                    receiver,
                    "TASK_DUE_SOON",
                    task.getTaskId(),
                    title,
                    message,
                    link,
                    actor // ✅ sender chính là actor
            );
        }

        log.info("🔔 [Deadline] Đã gửi 'TASK_DUE_SOON' cho {} người trong task '{}'",
                filtered.size(), task.getTitle());

    } catch (Exception e) {
        log.error("❌ sendDeadlineNotification() failed: {}", e.getMessage(), e);
    }
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

        long total = raw.stream()
                .mapToLong(m -> ((Number) m.get("count")).longValue())
                .sum();

        Map<String, Double> temp = new HashMap<>();
        raw.forEach(m -> {
            String status = (String) m.get("status");
            long count = ((Number) m.get("count")).longValue();
            double percent = total == 0 ? 0 : ((double) count / total) * 100;
            temp.put(status, percent);
        });

        // ✅ Ensure all keys exist
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

            // 🧮 Calculate weighted performance score
            double score = 0;
            if (dto.getTotalTasks() > 0) {
                double completion = ((double) dto.getCompletedTasks() / dto.getTotalTasks()) * 50;
                double punctuality = dto.getCompletedTasks() > 0
                        ? ((double) dto.getOnTimeTasks() / dto.getCompletedTasks()) * 30
                        : 0;
                double effort = ((double) dto.getPriorityPoints() / dto.getTotalTasks()) * 20;
                score = completion + punctuality + effort;
                if (score > 100) score = 100;
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
    public Page<Task> getUserTasksPaged(User user, String sortBy, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);

        // If status is provided -> FILTER, not sort
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return taskRepository.findUserTasksByStatus(user, status.toUpperCase(), pageable);
        }

        // Sorting path (no status filter)
        String key = (sortBy == null || sortBy.isBlank()) ? "deadline" : sortBy.toLowerCase();
        return switch (key) {
            case "priority" -> taskRepository.findUserTasksOrderByPriority(user, pageable);
            case "project"  -> taskRepository.findUserTasksOrderByProject(user, pageable);
            case "deadline" -> taskRepository.findUserTasksOrderByDeadline(user, pageable);
            default         -> taskRepository.findUserTasksOrderByDeadline(user, pageable);
        };
    }
    @Override
    public List<Task> findUpcomingDeadlines(Long userId) {
        return taskRepository.findTopUpcoming(userId, PageRequest.of(0, 5));
    }

}
