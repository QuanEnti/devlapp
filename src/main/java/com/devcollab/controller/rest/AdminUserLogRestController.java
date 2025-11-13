package com.devcollab.controller.rest;

import com.devcollab.domain.Activity;
import com.devcollab.service.system.ActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
public class AdminUserLogRestController {

    private final ActivityService activityService;

    public AdminUserLogRestController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** 🧾 Fetch all activity logs for Admin */
    @GetMapping
    public Map<String, Object> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Activity> pageData = activityService.getPaginatedActivities(PageRequest.of(page, size));

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageData.getContent());
        response.put("currentPage", pageData.getNumber());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("pageSize", size);

        return response;
    }
    @GetMapping("/search")
    public Map<String, Object> searchLogs(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Activity> pageData = activityService.searchActivities(user, action, entityType, PageRequest.of(page, size));

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageData.getContent());
        response.put("currentPage", pageData.getNumber());
        response.put("totalItems", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("pageSize", size);
        return response;
    }

}