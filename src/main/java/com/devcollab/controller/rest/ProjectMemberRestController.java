package com.devcollab.controller.rest;

import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.dto.request.AddMemberRequest;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.AuthService;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm/members")
@RequiredArgsConstructor
public class ProjectMemberRestController {

    private final ProjectMemberService projectMemberService;
    private final ProjectMemberRepository projectMemberRepo;

    

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MemberDTO> getMembers(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "20") int limit) {
        return projectMemberService.getMembersByProject(projectId, limit);
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public List<MemberDTO> getAllMembersOfPm(Authentication auth) {
        String email = extractEmail(auth);
        return projectMemberService.getAllMembersByPmEmail(email);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        Page<MemberDTO> members = projectMemberService.getAllMembers(page, size, keyword);
        return ResponseEntity.ok(Map.of(
                "content", members.getContent(),
                "totalPages", members.getTotalPages(),
                "totalElements", members.getTotalElements(),
                "currentPage", members.getNumber()));
    }


    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> removeMemberFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId) {
        try {
            boolean removed = projectMemberService.removeMemberFromProject(projectId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Xóa thành viên khỏi dự án thành công!",
                    "status", removed));
        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống khi xóa thành viên khỏi dự án!"));
        }
    }
    
    @DeleteMapping("/remove-user/{userId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> removeUserFromAllProjects(
            @PathVariable Long userId,
            Authentication auth) {
        try {
            String pmEmail = extractEmail(auth);
            boolean removed = projectMemberService.removeUserFromAllProjectsOfPm(pmEmail, userId);
            if (removed) {
                return ResponseEntity.ok(Map.of(
                        "message", "Đã xóa thành viên khỏi tất cả dự án bạn quản lý!",
                        "status", true));
            } else {
                return ResponseEntity.ok(Map.of(
                        "message", "Không có dự án nào chứa thành viên này!",
                        "status", false));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Lỗi hệ thống khi xóa thành viên khỏi tất cả dự án!"));
        }
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            return oauth2Auth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }

    @PutMapping("/{projectId}/role/{userId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam String role) {

        try {
            boolean updated = projectMemberService.updateMemberRole(projectId, userId, role);
            if (updated) {
                return ResponseEntity.ok(Map.of("message", "Cập nhật vai trò thành công!"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Không thể cập nhật vai trò!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống khi cập nhật vai trò!"));
        }
    }
    
    @PutMapping("/project/{projectId}/member/{userId}/role")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam String role,
            Authentication auth) {

        String pmEmail = auth.getName();
        projectMemberService.updateMemberRole(projectId, userId, role, pmEmail);

        var updated = projectMemberRepo.findMembersByProject(projectId);
        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật vai trò thành công!",
                "members", updated));
    }

}
