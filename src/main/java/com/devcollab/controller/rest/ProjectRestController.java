package com.devcollab.controller.rest;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.request.ProjectCreateRequestDTO;
import com.devcollab.dto.response.ApiResponse;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectRestController {

    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping("/create")
    public ApiResponse<Project> createProject(
            @RequestBody ProjectCreateRequestDTO request,
            Authentication authentication) {

        String email = null;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof DefaultOAuth2User oauthUser) {
                email = oauthUser.getAttribute("email");
            } else {
                email = authentication.getName();
            }
        }

        if (email == null) {
            return ApiResponse.error("Bạn chưa đăng nhập", 401);
        }

        try {
            User creator = userService.getByEmail(email).orElse(null);
            if (creator == null) {
                return ApiResponse.error("Không tìm thấy người dùng: " + email, 404);
            }

            Project project = new Project();
            project.setName(request.getName());
            project.setDescription(request.getDescription());
            project.setPriority(request.getPriority()); // ✅ thêm dòng này

            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                project.setStartDate(LocalDate.parse(request.getStartDate()));
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                project.setDueDate(LocalDate.parse(request.getEndDate()));
            }

            project.setCreatedBy(creator);

            Project saved = projectService.createProject(project, creator.getUserId());
            return ApiResponse.success("Tạo project thành công", saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Đã xảy ra lỗi khi tạo project: " + e.getMessage());
        }

    }

    @GetMapping("/search")
    public ApiResponse<?> searchProjects(@RequestParam String query) {
        try {
            var results = projectService.searchProjectsByKeyword(query);
            return ApiResponse.success("Tìm thấy " + results.size() + " dự án", results);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Lỗi khi tìm kiếm: " + e.getMessage());
        }
    }

    @GetMapping("/{projectId}/role")
    public ApiResponse<?> getUserRoleInProject(@PathVariable Long projectId, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ApiResponse.error("Bạn chưa đăng nhập", 401);
            }

            String email;
            Object principal = authentication.getPrincipal();
            if (principal instanceof DefaultOAuth2User oauthUser) {
                email = oauthUser.getAttribute("email");
            } else {
                email = authentication.getName();
            }

            if (email == null) {
                return ApiResponse.error("Không xác định được người dùng", 400);
            }

            // ✅ Gọi hàm trong service để lấy role
            String role = projectService.getUserRoleInProjectByEmail(projectId, email);

            return ApiResponse.success("Lấy vai trò thành công",
                    java.util.Map.of("projectId", projectId, "role", role));

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Không thể lấy vai trò: " + e.getMessage());
        }
    }

}
