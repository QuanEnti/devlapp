package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;

    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final AppEventService appEventService;
    @Override
    public Task createTask(Task task, Long creatorId) {
        if (task == null || task.getTitle() == null || task.getTitle().isBlank()) {
            throw new BadRequestException("Tên task không được để trống");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người tạo task"));

        if (task.getProject() == null || task.getProject().getProjectId() == null)
            throw new BadRequestException("Task phải thuộc một dự án cụ thể");

        if (task.getColumn() == null || task.getColumn().getColumnId() == null)
            throw new BadRequestException("Task phải nằm trong một cột cụ thể");

        Project project = projectRepository.findById(task.getProject().getProjectId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
        BoardColumn column = boardColumnRepository.findById(task.getColumn().getColumnId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cột"));

        if (!column.getProject().getProjectId().equals(project.getProjectId())) {
            throw new BadRequestException("Cột không thuộc dự án này");
        }

        task.setProject(project);
        task.setColumn(column);
        task.setCreatedBy(creator);
        task.setStatus("OPEN");
        task.setPriority(task.getPriority() != null ? task.getPriority() : "MEDIUM");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setOrderIndex(task.getOrderIndex() != null ? task.getOrderIndex() : 0);

        Task saved = taskRepository.save(task);

        // Ghi log + event
        activityService.log("TASK", saved.getTaskId(), "CREATE", saved.getTitle());
        appEventService.publishTaskCreated(saved);
        if (saved.getAssignee() != null)
            notificationService.notifyTaskAssigned(saved);

        return saved;
    }

    @Override
    public Task updateTask(Long id, Task patch) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        if (patch.getTitle() != null && !patch.getTitle().isBlank())
            existing.setTitle(patch.getTitle());
        if (patch.getDescriptionMd() != null)
            existing.setDescriptionMd(patch.getDescriptionMd());
        if (patch.getPriority() != null)
            existing.setPriority(patch.getPriority());
        if (patch.getDeadline() != null)
            existing.setDeadline(patch.getDeadline());

        existing.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(existing);

        activityService.log("TASK", saved.getTaskId(), "UPDATE", saved.getTitle());
        appEventService.publishTaskUpdated(saved);
        return saved;
    }

    @Override
    public Task assignTask(Long taskId, Long assigneeId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        task.setAssignee(assignee);
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "ASSIGN", assignee.getName());
        notificationService.notifyTaskAssigned(saved);
        return saved;
    }

    @Override
    public Task moveTask(Long taskId, Long columnId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        BoardColumn newCol = boardColumnRepository.findById(columnId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cột mới"));

        if (!newCol.getProject().getProjectId().equals(task.getProject().getProjectId())) {
            throw new BadRequestException("Cột không thuộc cùng dự án");
        }

        BoardColumn oldCol = task.getColumn();
        task.setColumn(newCol);
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "MOVE", oldCol.getName() + " → " + newCol.getName());
        appEventService.publishTaskMoved(saved, oldCol.getName(), newCol.getName());
        return saved;
    }

    @Override
    public Task closeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        if ("CLOSED".equalsIgnoreCase(task.getStatus())) {
            throw new BadRequestException("Task đã được đóng trước đó");
        }

        task.setStatus("CLOSED");
        task.setClosedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "CLOSE", task.getTitle());
        notificationService.notifyTaskClosed(saved);
        appEventService.publishTaskClosed(saved);
        return saved;
    }

    @Override
    public Task reopenTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        if (!"CLOSED".equalsIgnoreCase(task.getStatus())) {
            throw new BadRequestException("Chỉ có thể mở lại task đã đóng");
        }

        task.setStatus("OPEN");
        task.setClosedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);

        activityService.log("TASK", taskId, "REOPEN", task.getTitle());
        appEventService.publishTaskReopened(saved);
        return saved;
    }

    @Override
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new NotFoundException("Task không tồn tại");
        }
        taskRepository.deleteById(id);
        activityService.log("TASK", id, "DELETE", "Hard delete");
        appEventService.publishTaskDeleted(taskRepository.findById(id).orElse(null));
    }

    @Override
    public List<Task> getTasksByProject(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án"));
        return taskRepository.findByProject_ProjectId(projectId);
    }


    @Override
    public List<Task> getTasksByAssignee(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user"));
        return taskRepository.findByAssignee_UserId(userId);
    }

    @Override
    public List<Task> getTasksByProjectAndMember(Long projectId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
        return taskRepository.findByProject_ProjectIdAndAssignee_UserId(projectId, user.getUserId());
    }
}
