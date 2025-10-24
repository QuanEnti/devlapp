package com.devcollab.controller.rest;

import com.devcollab.domain.ProjectTarget;
import com.devcollab.domain.User;
import com.devcollab.dto.response.ApiResponse;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.ProjectTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm/targets")
@RequiredArgsConstructor
public class ProjectTargetController {

    private final ProjectTargetService projectTargetService;
    private final UserService userService; 
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @GetMapping("/{year}")
    public ApiResponse<List<ProjectTarget>> getTargetsByYear(
            @PathVariable int year,
            Authentication auth) {

        Long pmId = getUserIdFromAuth(auth);
        List<ProjectTarget> list = projectTargetService.getTargetsByYearAndPm(year, pmId);
        return ApiResponse.success(list);
    }

    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    @PostMapping("/set")
    public ApiResponse<ProjectTarget> setTarget(
            @RequestBody Map<String, Object> req,
            Authentication auth) {

        Long pmId = getUserIdFromAuth(auth);
        int month = ((Number) req.get("month")).intValue();
        int year = ((Number) req.get("year")).intValue();
        int targetCount = ((Number) req.get("targetCount")).intValue();

        ProjectTarget saved = projectTargetService.saveOrUpdateTarget(month, year, targetCount, pmId);
        return ApiResponse.success(saved);
    }

    private Long getUserIdFromAuth(Authentication auth) {
        String email;

        if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            email = oauth2Auth.getPrincipal().getAttribute("email");
        } else {
            email = auth.getName();
        }

        User user = userService.getByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

        return user.getUserId();
    }
}
