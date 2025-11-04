package com.devcollab.controller.rest;

import com.devcollab.dto.ActivityDTO;
import com.devcollab.dto.CommentDTO;
import com.devcollab.domain.User;
import com.devcollab.service.feature.CommentService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/{taskId}/activity")
@RequiredArgsConstructor
public class ActivityRestController {

    private final ActivityService activityService;
    private final CommentService commentService;
    private final AuthService authService;

    // 🟩 Lấy toàn bộ activity + comment
    @GetMapping
    public Map<String, Object> getTaskActivity(@PathVariable Long taskId) {
        List<ActivityDTO> activities = activityService.getActivities("TASK", taskId);
        List<CommentDTO> comments = commentService.getCommentsByTask(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("activityLogs", activities);
        result.put("comments", comments);
        return result;
    }

    // 🟦 Ghi log mới (thường không cần frontend gọi)
    @PostMapping
    public void addActivity(@PathVariable Long taskId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        User actor = authService.getCurrentUserEntity(auth);
        String action = payload.get("action");
        String message = payload.getOrDefault("dataJson", null);
        activityService.log("TASK", taskId, action, message, actor);
    }
}
