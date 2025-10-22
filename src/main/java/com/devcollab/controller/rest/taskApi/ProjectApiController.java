package com.devcollab.controller.rest.taskApi;

import com.devcollab.dto.taskDto.TaskListItemDto;
import com.devcollab.service.taskService.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectApiController {
    private final TaskService taskService;
    /**
     * Endpoint để lấy tất cả task của 1 project.
     * Hỗ trợ filter:
     * - /api/projects/1/tasks?filter=ALL  (hoặc không ?filter)
     * - /api/projects/1/tasks?filter=ME
     */
    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<?> getProjectTasks(
            @PathVariable Long projectId,
            @RequestAttribute("userId") Long currentUserId,
            // Nhận filter từ query param, mặc định là "ALL"
            @RequestParam(value = "filter", defaultValue = "ALL") String filter
    ) {
        try {
            // Chuyển đổi String sang Enum
            TaskService.TaskFilter taskFilter;
            if ("ME".equalsIgnoreCase(filter)) {
                taskFilter = TaskService.TaskFilter.ME;
            } else {
                taskFilter = TaskService.TaskFilter.ALL;
            }

            // Gọi service
            List<TaskListItemDto> tasks = taskService.getTasksByProject(projectId, currentUserId, taskFilter);
            return ResponseEntity.ok(tasks);

        } catch (AccessDeniedException e) {
            // Lỗi 403 nếu user không phải thành viên
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            // Các lỗi chung
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
