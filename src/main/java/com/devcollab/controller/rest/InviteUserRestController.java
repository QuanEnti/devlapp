package com.devcollab.controller.rest;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.JoinRequestService;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.system.ProjectMemberService;
import com.devcollab.service.system.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý mời người dùng, chia sẻ link, join dự án (Project-level permission)
 */
@RestController
@RequestMapping("/api/pm/invite")
@RequiredArgsConstructor
public class InviteUserRestController {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMemberRepository projectMemberRepo;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final ProjectAuthorizationService authz;
    private final JoinRequestService joinRequestService;

    @PostMapping
    public ResponseEntity<?> inviteToProject(@RequestParam Long projectId,
            @RequestParam String email, @RequestParam(defaultValue = "Member") String role,
            Authentication auth) {
        try {
            String pmEmail = extractEmail(auth);
            // ✅ Phân quyền đã được kiểm tra trong service layer
            projectMemberService.addMemberToProject(projectId, pmEmail, email, role);
            return ResponseEntity.ok(Map.of("message", "Đã mời thành viên vào dự án"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getAllProjects(Authentication auth) {
        String email = extractEmail(auth);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user: " + email));

        var projects = projectService.getProjectsByUser(user.getUserId());
        return ResponseEntity.ok(projects.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", p.getProjectId());
            map.put("name", p.getName());
            map.put("status", p.getStatus());
            map.put("allowLinkJoin", p.isAllowLinkJoin());
            map.put("inviteLink", p.getInviteLink()); // Can be null
            return map;
        }).toList());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectDetails(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);
        if (!authz.isMemberOfProject(email, projectId)) {
            throw new AccessDeniedException("Bạn không thuộc dự án này");
        }

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));
        List<MemberDTO> members = projectMemberRepo.findMembersByProject(projectId);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", project.getProjectId());
        result.put("name", project.getName());
        result.put("members", members != null ? members : List.of());
        result.put("inviteLink", project.getInviteLink());
        result.put("allowLinkJoin", project.isAllowLinkJoin());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/project/{projectId}/share/enable")
    public ResponseEntity<?> enableShareLink(@PathVariable Long projectId, Authentication auth) {
        try {
            String email = extractEmail(auth);
            // ✅ Phân quyền đã được kiểm tra trong service layer
            Project updated = projectService.enableShareLink(projectId, email);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Đã bật chia sẻ dự án!");
            result.put("inviteLink", updated.getInviteLink());
            result.put("allowLinkJoin", updated.isAllowLinkJoin());
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/project/{projectId}/share/disable")
    public ResponseEntity<?> disableShareLink(@PathVariable Long projectId, Authentication auth) {
        try {
            String email = extractEmail(auth);
            // ✅ Phân quyền đã được kiểm tra trong service layer
            Project updated = projectService.disableShareLink(projectId, email);
            return ResponseEntity.ok(Map.of("message", "Đã tắt chia sẻ dự án!", "allowLinkJoin",
                    updated.isAllowLinkJoin()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}/share/link")
    public ResponseEntity<?> getShareLink(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);
        if (!authz.isMemberOfProject(email, projectId)) {
            throw new AccessDeniedException("Bạn không thuộc dự án này");
        }

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        Map<String, Object> result = new HashMap<>();
        result.put("allowLinkJoin", project.isAllowLinkJoin());
        result.put("inviteLink", project.getInviteLink()); // Can be null for new projects
        return ResponseEntity.ok(result);
    }

    @PostMapping("/project/{projectId}/share/copy")
    public ResponseEntity<?> copyShareLink(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);
        if (!authz.isMemberOfProject(email, projectId)) {
            throw new AccessDeniedException("Bạn không thuộc dự án này");
        }

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        if (!project.isAllowLinkJoin() || project.getInviteLink() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Link sharing chưa được bật hoặc chưa có link!"));
        }

        // ✅ Lưu email người copy link để kiểm tra quyền khi join
        // Nếu member copy → người join sẽ vào join request
        // Nếu PM copy → người join sẽ vào trực tiếp
        project.setInviteCreatedBy(email);
        projectRepo.save(project);

        // Kiểm tra role để thông báo cho UI
        String userRole = authz.getRoleInProject(email, projectId);
        boolean isPm = "PM".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole);
        String message = isPm ? "Link đã được copy. Người được mời sẽ tham gia trực tiếp."
                : "Link đã được copy. Người được mời sẽ cần được PM duyệt.";

        Map<String, Object> result = new HashMap<>();
        result.put("inviteLink", project.getInviteLink());
        result.put("message", message);
        result.put("requiresApproval", !isPm);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/join/{inviteLink}")
    public ResponseEntity<?> joinByInviteLink(@PathVariable String inviteLink,
            Authentication auth) {

        String email = extractEmail(auth);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user!"));

        try {
            Map<String, Object> result =
                    projectService.joinProjectByLink(inviteLink, user.getUserId());

            String message = (String) result.get("message");

            if ("joined_success".equals(message)) {
                return ResponseEntity.ok(result);
            }

            if ("join_request_sent".equals(message)) {
                return ResponseEntity.ok(result);
            }

            return ResponseEntity.ok(result);

        } catch (BadRequestException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/project/{projectId}/join-requests")
    public ResponseEntity<?> getJoinRequests(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);
        authz.ensurePmOfProject(email, projectId);
        var requests = joinRequestService.getPendingRequests(projectId);

        var dtoList = requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("requestId", r.getId());
            map.put("userEmail", r.getUser().getEmail());
            map.put("userName", r.getUser().getName());
            map.put("createdAt", r.getCreatedAt());
            map.put("status", r.getStatus());
            if (r.getReviewedBy() != null) {
                map.put("reviewedBy", r.getReviewedBy());
            }
            if (r.getReviewedAt() != null) {
                map.put("reviewedAt", r.getReviewedAt());
            }
            return map;
        }).toList();

        return ResponseEntity.ok(dtoList);
    }


    @PostMapping("/join-requests/{requestId}/approve")
    public ResponseEntity<?> approveJoinRequest(@PathVariable Long requestId, Authentication auth) {
        String email = extractEmail(auth);
        var result = joinRequestService.approveRequest(requestId, email);
        Long projectId = result.getProject() != null ? result.getProject().getProjectId() : null;
        return ResponseEntity.ok(Map.of("message", "Yêu cầu đã được duyệt", "projectId", projectId,
                "status", result.getStatus()));
    }


    @PostMapping("/join-requests/{requestId}/reject")
    public ResponseEntity<?> rejectJoinRequest(@PathVariable Long requestId, Authentication auth) {
        String email = extractEmail(auth);
        try {
            joinRequestService.rejectRequest(requestId, email);
            return ResponseEntity.ok(Map.of("message", "Đã từ chối yêu cầu tham gia"));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/project/{projectId}/member/{userId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long projectId,
            @PathVariable Long userId, @RequestParam String role, Authentication auth) {
        try {
            String email = extractEmail(auth);
            authz.ensurePmOfProject(email, projectId);
            projectMemberService.updateMemberRole(projectId, userId, role);
            return ResponseEntity.ok(Map.of("message", " Vai trò đã được cập nhật!", "projectId",
                    projectId, "userId", userId, "role", role));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/search-users")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<User> users = userRepo.searchUsersByKeyword(keyword.trim());
        var result = users.stream().limit(10).map(u -> Map.of("userId", u.getUserId(), "name",
                u.getName(), "email", u.getEmail(), "avatarUrl", u.getAvatarUrl())).toList();

        return ResponseEntity.ok(result);
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }
}
