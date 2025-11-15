package com.devcollab.controller.rest;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.ProjectSummaryDTO;
import com.devcollab.dto.request.ProjectCreateRequestDTO;
import com.devcollab.dto.response.ApiResponse;
import com.devcollab.dto.response.ProjectResponseDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Objects;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectRestController {

    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping("/create")
    public ApiResponse<Project> createProject(@RequestBody ProjectCreateRequestDTO request,
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
            System.out.println("Creating project with coverImage:" + request.getCoverImage());

            // ✅ Kiểm tra xem người dùng đã có project cùng tên chưa
            boolean exists = projectService.existsByNameAndCreatedBy_UserId(request.getName(),
                    creator.getUserId());
            if (exists) {
                return ApiResponse.error("Bạn đã tạo project này rồi!", 400);
            }

            // ✅ Nếu chưa có, tiếp tục tạo mới
            Project project = new Project();
            project.setName(request.getName());
            project.setDescription(request.getDescription());
            project.setPriority(request.getPriority());
            project.setCoverImage(request.getCoverImage());

            // Set status nếu có, nếu không service sẽ set mặc định là "Active"
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                project.setStatus(request.getStatus());
            }

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
    public ApiResponse<?> getUserRoleInProject(@PathVariable Long projectId,
            Authentication authentication) {
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

    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }

    @GetMapping("")
    public Page<ProjectSummaryDTO> getUserProjects(Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        String email = getEmailFromAuthentication(auth);
        return projectService.getProjectsByUserPaginated(email, page, size);
    }

    @PutMapping("/{projectId}")
    public ApiResponse<ProjectResponseDTO> updateProject(@PathVariable Long projectId,
            @RequestBody ProjectCreateRequestDTO request, Authentication authentication) {
        String email = getEmailFromAuthentication(authentication);
        if (email == null) {
            return ApiResponse.error("Bạn chưa đăng nhập", 401);
        }

        try {
            User editor = userService.getByEmail(email).orElse(null);
            if (editor == null) {
                return ApiResponse.error("Không tìm thấy người dùng: " + email, 404);
            }

            Project existing = projectService.getById(projectId);
            // ✅ Chỉ người tạo project được phép chỉnh sửa
            if (!Objects.equals(existing.getCreatedBy().getUserId(), editor.getUserId())) {
                return ApiResponse.error("Bạn không có quyền chỉnh sửa project này!", 403);
            }
            // ✅ Tạo bản patch object để cập nhật
            Project patch = new Project();
            if (request.getName() != null) {
                patch.setName(request.getName());
            }
            if (request.getDescription() != null) {
                patch.setDescription(request.getDescription());
            }
            if (request.getBusinessRule() != null) {
                patch.setBusinessRule(request.getBusinessRule());
            }
            if (request.getPriority() != null) {
                patch.setPriority(request.getPriority());
            }
            if (request.getStatus() != null) {
                patch.setStatus(request.getStatus());
            }
            System.out.println("CverImg: " + request.getCoverImage());
            if (request.getCoverImage() != null) {
                patch.setCoverImage(request.getCoverImage());
            }
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                patch.setStartDate(LocalDate.parse(request.getStartDate()));
            }
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                patch.setDueDate(LocalDate.parse(request.getEndDate()));
            }

            Project updated = projectService.updateProject(projectId, patch);
            ProjectResponseDTO dto = new ProjectResponseDTO(updated.getProjectId(),
                    updated.getName(), updated.getDescription(), updated.getBusinessRule(),
                    updated.getPriority(), updated.getStatus(), updated.getVisibility(),
                    updated.getStartDate(), updated.getDueDate(),
                    updated.getCreatedBy() != null ? updated.getCreatedBy().getEmail() : null,
                    updated.getCoverImage());
            return ApiResponse.success("Cập nhật project thành công", dto);

        } catch (BadRequestException | NotFoundException e) {
            return ApiResponse.error(e.getMessage(), 400);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Đã xảy ra lỗi khi cập nhật project: " + e.getMessage());
        }
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<?> deleteProject(@PathVariable Long projectId,
            Authentication authentication) {

        String email = getEmailFromAuthentication(authentication);
        if (email == null) {
            return ApiResponse.error("Bạn chưa đăng nhập", 401);
        }

        try {
            User currentUser = userService.getByEmail(email).orElse(null);
            if (currentUser == null) {
                return ApiResponse.error("Không tìm thấy người dùng: " + email, 404);
            }

            Project project = projectService.getById(projectId);

            // ✅ Only project creator can delete
            if (!Objects.equals(project.getCreatedBy().getUserId(), currentUser.getUserId())) {
                return ApiResponse.error("Bạn không có quyền xóa project này!", 403);
            }

            projectService.deleteProject(projectId);
            return ApiResponse.success("Đã xóa project thành công");

        } catch (NotFoundException e) {
            return ApiResponse.error(e.getMessage(), 404);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("Đã xảy ra lỗi khi xóa project: " + e.getMessage());
        }
    }



}
