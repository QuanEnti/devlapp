package com.devcollab.controller.rest.taskApi;

import com.devcollab.dto.taskDto.TaskDetailDto;
import com.devcollab.service.taskService.TaskService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskApiController {
    private final TaskService taskService;

    /**
     * Endpoint để lấy chi tiết một task
     * Ví dụ: GET /api/tasks/123
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Long taskId,
            @RequestAttribute("userId") Long currentUserId // Lấy từ JwtAuthenticationFilter
    ) {
        try {
            // Gọi service với ID task và ID user (để check quyền)
            TaskDetailDto taskDetail = taskService.getTaskDetails(taskId, currentUserId);
            return ResponseEntity.ok(taskDetail);

        } catch (EntityNotFoundException e) {
            // Nếu service ném lỗi không tìm thấy
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (AccessDeniedException e) {
            // Nếu service ném lỗi không có quyền
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch (Exception e) {
            // Các lỗi khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
