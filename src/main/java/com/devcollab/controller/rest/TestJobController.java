package com.devcollab.controller.rest;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devcollab.service.system.TaskDeadlineReminderJob;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestJobController {

    private final TaskDeadlineReminderJob reminderJob;

    @GetMapping("/deadline-job")
    public ResponseEntity<String> runJobNow() {
        reminderJob.checkUpcomingDeadlines();
        return ResponseEntity.ok("✅ Đã chạy job thủ công lúc " + LocalDateTime.now());
    }
    
}
