package com.devcollab.controller.rest;

import com.devcollab.dto.ActivityDTO;
import com.devcollab.service.system.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/activity")
@RequiredArgsConstructor
public class ProjectActivityController {

    private final ActivityService activityService;

    // 🟩 Lấy toàn bộ activity của 1 project
    @GetMapping
    public List<ActivityDTO> getProjectActivity(@PathVariable Long projectId) {
        return activityService.getActivities("PROJECT", projectId);
    }
}
