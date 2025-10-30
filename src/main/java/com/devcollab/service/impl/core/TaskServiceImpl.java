package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.request.MoveTaskRequest;
import com.devcollab.dto.request.TaskDatesUpdateReq;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.TaskService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.devcollab.dto.userTaskDto.TaskCardDTO;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final ProjectRepository projectRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final TaskRepository taskRepository;

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

        return taskRepository.save(task);
    }
    
    @Override
    public Task quickCreate(String title, Long columnId, Long projectId, Long creatorId) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Tiêu đề task không được để trống");
        }

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

        return taskRepository.save(task);
    }

    @Override
    public Task createTask(Task task, Long creatorId) {
        if (task == null)
            throw new BadRequestException("Task rỗng");

        // Gán mặc định
        if (task.getCreatedAt() == null)
            task.setCreatedAt(LocalDateTime.now());
        if (task.getUpdatedAt() == null)
            task.setUpdatedAt(LocalDateTime.now());
        if (task.getStatus() == null)
            task.setStatus("OPEN");

        return taskRepository.save(task);
    }

    // ----------------------------------------------------
    // ✅ 3. Cập nhật Task
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
        return taskRepository.save(existing);
    }

    // ----------------------------------------------------
    // ✅ 4. Xóa Task
    // ----------------------------------------------------
    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id))
            throw new NotFoundException("Task không tồn tại");
        taskRepository.deleteById(id);
    }

    // ----------------------------------------------------
    // ✅ 5. Gán người phụ trách
    // ----------------------------------------------------
    @Override
    public Task assignTask(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        User assignee = new User();
        assignee.setUserId(assigneeId);
        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ----------------------------------------------------
    // ✅ 6. Di chuyển Task sang cột khác
    // ----------------------------------------------------
    @Override
    public Task moveTask(Long taskId, Long columnId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        BoardColumn column = new BoardColumn();
        column.setColumnId(columnId);

        task.setColumn(column);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ----------------------------------------------------
    // ✅ 7. Đóng / mở lại Task
    // ----------------------------------------------------
    @Override
    public Task closeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setStatus("CLOSED");
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Override
    public Task reopenTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setStatus("OPEN");
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    // ----------------------------------------------------
    // ✅ 8. Truy vấn
    // ----------------------------------------------------
    @Override
public List<TaskDTO> getTasksByProject(Long projectId) {
    projectRepository.findById(projectId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));

    return taskRepository.findByProject_ProjectId(projectId)
        .stream()
        .map(TaskDTO::fromEntity) // ✅ chuyển entity sang DTO
        .collect(Collectors.toList());
}

    @Override
    public List<Task> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssignee_UserId(userId);
    }
    
    @Override
    public TaskDTO getByIdAsDTO(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        return TaskDTO.fromEntity(task);
    }

    @Override
    public List<Task> getTasksByProjectAndMember(Long projectId, String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTasksByProjectAndMember'");
    }

    @Override
    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
    }

    @Override
    public Task updateTaskDescription(Long id, String description) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        task.setDescriptionMd(description);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
    
    @Override
    @Transactional
    public TaskDTO updateDates(Long taskId, TaskDTO dto) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("❌ Task not found with ID: " + taskId));

        DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        System.out.printf("📅 UpdateDates → taskId=%d, start=%s, deadline=%s, recurring=%s, reminder=%s%n",
                taskId, dto.getStartDate(), dto.getDeadline(), dto.getRecurring(), dto.getReminder());

        // ===== START DATE =====
        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            try {
                LocalDateTime parsedStart = LocalDateTime.parse(dto.getStartDate(), iso);
                if (task.getStartDate() == null || !task.getStartDate().equals(parsedStart)) {
                    task.setStartDate(parsedStart);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Parse startDate fail: " + dto.getStartDate());
            }
        } else {
            if (task.getStartDate() != null)
                task.setStartDate(null);
        }

        // ===== DEADLINE =====
        if (dto.getDeadline() != null && !dto.getDeadline().isBlank()) {
            try {
                LocalDateTime parsedDeadline = LocalDateTime.parse(dto.getDeadline(), iso);
                if (task.getDeadline() == null || !task.getDeadline().equals(parsedDeadline)) {
                    task.setDeadline(parsedDeadline);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Parse deadline fail: " + dto.getDeadline());
            }
        } else {
            if (task.getDeadline() != null)
                task.setDeadline(null);
        }

        // ===== RECURRENCE & REMINDER =====
        String recurring = dto.getRecurring() != null ? dto.getRecurring() : "Never";
        String reminder = dto.getReminder() != null ? dto.getReminder() : "Never";

        if (!recurring.equals(task.getRecurring()))
            task.setRecurring(recurring);
        if (!reminder.equals(task.getReminder()))
            task.setReminder(reminder);

        // ===== Updated time =====
        task.setUpdatedAt(LocalDateTime.now());

        // ===== Save =====
        taskRepository.save(task);

        System.out.println("✅ Task dates updated successfully → " + task.getTaskId());
        return TaskDTO.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskDTO moveTask(Long taskId, MoveTaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        BoardColumn newCol = boardColumnRepository.findById(req.getTargetColumnId())
                .orElseThrow(() -> new RuntimeException("Target column not found"));

        // Cập nhật cột & vị trí
        task.setColumn(newCol);
        task.setOrderIndex(req.getNewOrderIndex());
        task.setUpdatedAt(LocalDateTime.now());

        taskRepository.save(task);
        return TaskDTO.fromEntity(task);
    }

    @Override
    public List<TaskCardDTO> getUserTasks(Long userId, Long projectId, String statuses) {
        return taskRepository.findUserTasks(userId, projectId, statuses);
    }


}
