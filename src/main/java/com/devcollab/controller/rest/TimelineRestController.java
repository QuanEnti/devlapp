package com.devcollab.controller.rest;

import com.devcollab.dto.TimelineTaskDTO;
import com.devcollab.service.core.TimelineService;
import com.devcollab.service.system.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
public class TimelineRestController {

    private final TimelineService timelineService;
    private final AuthService authService;

    @GetMapping("/tasks")
    public ResponseEntity<List<TimelineTaskDTO>> getTimelineTasks(Authentication auth) {
        try {
            var currentUser = authService.getCurrentUser(auth);
            if (currentUser == null || currentUser.getUserId() == null) {
                return ResponseEntity.status(401).build();
            }

            List<TimelineTaskDTO> tasks = timelineService.getTasksForUser(currentUser.getUserId());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}

