package com.devcollab.controller.rest;

import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm/members")
@RequiredArgsConstructor
public class ProjectMemberRestController {

    private final ProjectMemberService projectMemberService;
    private final ProjectMemberRepository projectMemberRepo;

    // 🟢 Lấy danh sách thành viên trong 1 project (ai cũng xem được nếu là member)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MemberDTO> getMembers(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(required = false) String keyword) {
        return projectMemberService.getMembersByProject(projectId, limit, keyword);
    }

    // 🧭 Tổng quan tất cả members của PM
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public List<MemberDTO> getAllMembersOfPm(Authentication auth) {
        String email = extractEmail(auth);
        return projectMemberService.getAllMembersByPmEmail(email);
    }

    // 🧩 Danh sách tất cả members có trong các project (phân trang)
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

    // 🔥 Xóa 1 member khỏi project (phân quyền theo project)
    @DeleteMapping("/{projectId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeMemberFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication auth) {
        try {
            String requesterEmail = extractEmail(auth);
            boolean removed = projectMemberService.removeMemberFromProject(projectId, userId, requesterEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Xóa thành viên khỏi dự án thành công!",
                    "status", removed));

        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi hệ thống khi xóa thành viên khỏi dự án!"));
        }
    }

    // ❌ Xóa user khỏi tất cả project mà PM sở hữu
    @DeleteMapping("/remove-user/{userId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> removeUserFromAllProjects(
            @PathVariable Long userId,
            Authentication auth) {
        try {
            String pmEmail = extractEmail(auth);
            boolean removed = projectMemberService.removeUserFromAllProjectsOfPm(pmEmail, userId);
            return ResponseEntity.ok(Map.of(
                    "message", removed
                            ? "Đã xóa thành viên khỏi tất cả dự án bạn quản lý!"
                            : "Không có dự án nào chứa thành viên này!",
                    "status", removed));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Lỗi hệ thống khi xóa thành viên khỏi tất cả dự án!"));
        }
    }

    // 🔄 Đổi role trong project (theo phân quyền Project)
    @PutMapping("/project/{projectId}/member/{userId}/role")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @RequestParam String role,
            Authentication auth) {
        try {
            String pmEmail = extractEmail(auth);
            projectMemberService.updateMemberRole(projectId, userId, role, pmEmail);

            var updated = projectMemberRepo.findMembersByProject(projectId);
            return ResponseEntity.ok(Map.of(
                    "message", "✅ Cập nhật vai trò thành công!",
                    "members", updated));

        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Lỗi hệ thống khi cập nhật vai trò!"));
        }
    }

    // 🧠 Helper lấy email từ Auth (hỗ trợ cả OAuth2)
    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            return oauth2Auth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }
}
