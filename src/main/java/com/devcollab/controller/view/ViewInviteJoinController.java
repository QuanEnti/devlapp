package com.devcollab.controller.view;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/join")
@RequiredArgsConstructor
public class ViewInviteJoinController {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMemberRepository projectMemberRepo;

    @GetMapping("/{inviteLink}")
    public String handleInvite(
            @PathVariable String inviteLink,
            Authentication auth,
            Model model) {
        var projectOpt = projectRepo.findActiveSharedProject(inviteLink);
        if (projectOpt.isEmpty()) {
            // Link hỏng/tắt → cho về trang signin (hoặc 404 tùy bạn)
            return "redirect:/view/signin?error=invalid-invite";
        }
        Project project = projectOpt.get();

        // Chưa đăng nhập? → sang signin và giữ lại redirect
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/view/signin?redirect=/join/" + inviteLink;
        }

        // Đã đăng nhập → kiểm tra membership
        String email = (auth instanceof OAuth2AuthenticationToken o)
                ? o.getPrincipal().getAttribute("email")
                : auth.getName();

        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null) {
            boolean isMember = projectMemberRepo
                    .existsByProject_ProjectIdAndUser_UserId(project.getProjectId(), user.getUserId());
            if (isMember) {
                // ✅ ĐÃ LÀ THÀNH VIÊN → nhảy thẳng vào board
                return "redirect:/view/pm/project/board?projectId=" + project.getProjectId();
            }
        }

        // ❗ Chưa là member → hiển thị trang join
        model.addAttribute("inviteLink", inviteLink);
        model.addAttribute("projectName", project.getName());
        model.addAttribute("ownerName", project.getCreatedBy() != null ? project.getCreatedBy().getName() : "PM");
        return "project/join-project";
    }
}
