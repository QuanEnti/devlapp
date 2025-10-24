package com.devcollab.controller.rest;

import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.response.*;
import com.devcollab.service.core.ProjectService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pm/project")
@RequiredArgsConstructor
public class ProjectDashboardRestController {

    private final ProjectService projectService;

    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/{id}/dashboard")
    public ApiResponse<ProjectDashboardDTO> getProjectDashboard(
            @PathVariable("id") Long projectId,
            Authentication auth) {

        String email = extractEmail(auth);
        ProjectDashboardDTO dto = projectService.getDashboardForPm(projectId, email);
        return ApiResponse.success(dto);
    }

    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/{id}/performance")
    public ApiResponse<ProjectPerformanceDTO> getProjectPerformance(
            @PathVariable("id") Long projectId,
            Authentication auth) {

        String email = extractEmail(auth);
        ProjectPerformanceDTO dto = projectService.getPerformanceData(projectId, email);
        return ApiResponse.success(dto);
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            return oauth2Auth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }
    
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/top")
    public ApiResponse<List<ProjectDTO>> getTopProjects(
            @RequestParam(defaultValue = "9") int limit) {

        List<ProjectDTO> projects = projectService.getTopProjects(limit);
        return ApiResponse.success(projects);
    }
    
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ApiResponse<Page<ProjectDTO>> getAllProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String keyword) {
        Page<ProjectDTO> projects = projectService.getAllProjects(page, size, keyword);
        return ApiResponse.success(projects);
    }
}
