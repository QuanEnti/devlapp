package com.devcollab.service.taskService;


import com.devcollab.domain.Task;
import com.devcollab.dto.taskDto.TaskDetailDto;
import com.devcollab.repository.taskRepository.ProjectMemberRepository;
import com.devcollab.repository.taskRepository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository; // <-- Dùng để check bảo mật

    public TaskDetailDto getTaskDetails(Long taskId, Long currentUserId) throws AccessDeniedException {

        // B1: Lấy Task bằng query đã tối ưu (EntityGraph)
        Task task = taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // B2: KIỂM TRA BẢO MẬT (Rất quan trọng)
        // User chỉ được xem task nếu họ là thành viên của project đó
        Long projectId = task.getProject().getProjectId();

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId);

        if (!isMember) {
            // Nếu không phải thành viên, ném lỗi 403 Forbidden
            throw new AccessDeniedException("User is not a member of the project for this task");
        }

        // B3: Nếu qua được, map Entity sang DTO và trả về
        return TaskDetailDto.fromEntity(task);
    }

}
