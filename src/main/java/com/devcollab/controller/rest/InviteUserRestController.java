package com.devcollab.controller.rest;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
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
    private final UserRepository userRepository;

    /**
     * 🟢 Mời thành viên vào dự án — chỉ PM của project đó mới được phép
     */
    @PostMapping
    public ResponseEntity<?> inviteToProject(
            @RequestParam Long projectId,
            @RequestParam String email,
            @RequestParam(defaultValue = "Member") String role,
            Authentication auth) {
        try {
            String pmEmail = extractEmail(auth);
            authz.ensurePmOfProject(pmEmail, projectId); // ✅ chỉ PM trong project mới được mời

            projectMemberService.addMemberToProject(projectId, pmEmail, email, role);
            return ResponseEntity.ok(Map.of("message", "Đã mời thành viên vào dự án"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🧾 Lấy danh sách project mà user hiện tại là PM hoặc Member
     */
    @GetMapping("/projects")
    public ResponseEntity<?> getAllProjects(Authentication auth) {
        String email = extractEmail(auth);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user: " + email));

        var projects = projectService.getProjectsByUser(user.getUserId());
        return ResponseEntity.ok(projects.stream().map(p -> Map.of(
                "projectId", p.getProjectId(),
                "name", p.getName(),
                "status", p.getStatus(),
                "allowLinkJoin", p.isAllowLinkJoin(),
                "inviteLink", p.getInviteLink())));
    }

    /**
     * 👀 Xem chi tiết dự án — phải là thành viên của dự án mới được xem
     */
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

    /**
     * ✅ Bật chia sẻ link mời — chỉ PM được phép bật
     */
    @PostMapping("/project/{projectId}/share/enable")
    public ResponseEntity<?> enableShareLink(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);

        // 🔒 Chỉ PM được phép bật
        authz.ensurePmOfProject(email, projectId);

        Project updated = projectService.enableShareLink(projectId, email);
        return ResponseEntity.ok(Map.of(
                "message", "Đã bật chia sẻ dự án!",
                "inviteLink", updated.getInviteLink(),
                "allowLinkJoin", updated.isAllowLinkJoin()));
    }

    /**
     * 🔴 Tắt chia sẻ link mời — chỉ PM được phép tắt
     */
    @DeleteMapping("/project/{projectId}/share/disable")
    public ResponseEntity<?> disableShareLink(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);

        // 🔒 Chỉ PM được phép tắt
        authz.ensurePmOfProject(email, projectId);

        Project updated = projectService.disableShareLink(projectId, email);
        return ResponseEntity.ok(Map.of(
                "message", "Đã tắt chia sẻ dự án!",
                "allowLinkJoin", updated.isAllowLinkJoin()));
    }

    @GetMapping("/project/{projectId}/share/link")
    public ResponseEntity<?> getShareLink(@PathVariable Long projectId, Authentication auth) {
        String email = extractEmail(auth);
        if (!authz.isMemberOfProject(email, projectId)) {
            throw new AccessDeniedException("Bạn không thuộc dự án này");
        }

        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy dự án!"));

        Map<String, Object> resp = new HashMap<>();
        resp.put("allowLinkJoin", project.isAllowLinkJoin());
        resp.put("inviteLink", project.getInviteLink());
        return ResponseEntity.ok(resp);
    }

    /**
     * ✉️ Join project qua link mời (ai cũng được, miễn đăng nhập)
     */
    @PostMapping("/join/{inviteLink}")
    public ResponseEntity<?> joinByInviteLink(@PathVariable String inviteLink, Authentication auth) {
        String email = extractEmail(auth);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user!"));

        ProjectMember joined = projectService.joinProjectByLink(inviteLink, user.getUserId());
        var project = joined.getProject();

        return ResponseEntity.ok(Map.of(
                "message", "joined_success",
                "projectId", project.getProjectId(),
                "projectName", project.getName()));
    }

    /**
     * ✏️ Cập nhật vai trò thành viên trong dự án — chỉ PM được phép
     */
    @PutMapping("/project/{projectId}/member/{userId}/role")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam String role,
            Authentication auth) {
        try {
            String email = extractEmail(auth);
            authz.ensurePmOfProject(email, projectId);

            boolean updated = projectMemberService.updateMemberRole(projectId, userId, role);
            return ResponseEntity.ok(Map.of(
                    "message", "✅ Vai trò đã được cập nhật!",
                    "projectId", projectId,
                    "userId", userId,
                    "role", role));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search-users")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        List<User> users = userRepository.searchUsersByKeyword(keyword.trim());
        var result = users.stream()
                .limit(10)
                .map(u -> Map.of(
                        "userId", u.getUserId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "avatarUrl", u.getAvatarUrl()))
                .toList();

        return ResponseEntity.ok(result);
    }

    /** 🔍 Helper lấy email người dùng */
    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }
}
