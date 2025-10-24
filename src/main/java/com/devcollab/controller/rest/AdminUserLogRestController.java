package com.devcollab.controller.rest;

import com.devcollab.domain.Activity;
import com.devcollab.service.system.ActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminUserLogRestController {

    private final ActivityService activityService;

    public AdminUserLogRestController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** 🧾 Fetch all activity logs for Admin */
    @GetMapping
    public List<Activity> getAllLogs() {
        return activityService.getAllActivities();
    }
}
