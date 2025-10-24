package com.devcollab.controller.rest;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.dto.MemberDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.system.ProjectMemberService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý mời người dùng, chia sẻ link, join dự án
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

    @PostMapping
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> inviteToProject(
            @RequestParam Long projectId,
            @RequestParam String email,
            @RequestParam(defaultValue = "Member") String role,
            Authentication auth) {
        String pmEmail = auth.getName();
        projectMemberService.addMemberToProject(projectId, pmEmail, email, role);
        return ResponseEntity.ok(Map.of("message", "Đã mời thành viên vào dự án"));
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> getAllProjects(Authentication auth) {
        String pmEmail = extractEmail(auth);
        var pm = userRepo.findByEmail(pmEmail)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy PM: " + pmEmail));

        var projects = projectRepo.findByCreatedBy_UserId(pm.getUserId());
        return ResponseEntity.ok(projects.stream().map(p -> Map.of(
                "projectId", p.getProjectId(),
                "name", p.getName(),
                "status", p.getStatus(),
                "allowLinkJoin", p.isAllowLinkJoin(),
                "inviteLink", p.getInviteLink())));
    }

   @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> getProjectDetails(@PathVariable Long projectId) {
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


    /** 🟢 Bật chia sẻ link mời */
    @PostMapping("/project/{projectId}/share/enable")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> enableShareLink(@PathVariable Long projectId, Authentication auth) {
        String pmEmail = extractEmail(auth);
        Project updated = projectService.enableShareLink(projectId, pmEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Đã bật chia sẻ dự án!",
                "inviteLink", updated.getInviteLink(),
                "allowLinkJoin", updated.isAllowLinkJoin()));
    }

    /** 🔴 Tắt chia sẻ link mời */
    @DeleteMapping("/project/{projectId}/share/disable")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<?> disableShareLink(@PathVariable Long projectId, Authentication auth) {
        String pmEmail = extractEmail(auth);
        Project updated = projectService.disableShareLink(projectId, pmEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Đã tắt chia sẻ dự án!",
                "allowLinkJoin", updated.isAllowLinkJoin()));
    }

    /** 🟣 User join project qua link mời */
    @PostMapping("/join/{inviteLink}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> joinByInviteLink(@PathVariable String inviteLink, Authentication auth) {
        String email = extractEmail(auth);
        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user!"));

        ProjectMember newMember = projectService.joinProjectByLink(inviteLink, user.getUserId());
        return ResponseEntity.ok(Map.of(
                "message", "Tham gia dự án thành công!",
                "projectId", newMember.getProject().getProjectId(),
                "projectName", newMember.getProject().getName()));
    }

    private String extractEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            return oauth.getPrincipal().getAttribute("email");
        }
        return auth.getName();
    }
}
