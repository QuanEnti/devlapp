package com.devcollab.controller.view;

import com.devcollab.service.core.ProjectService;
import com.devcollab.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/view")
@RequiredArgsConstructor
public class ProjectViewController {

    private final ProjectService projectService;

    @GetMapping("/project/{projectId}/tasks")
    public String showMemberTasks(
            @PathVariable Long projectId,
            Model model,
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
            return "redirect:/view/signin";
        }

        Project project = projectService.getById(projectId);
        model.addAttribute("project", project);
        model.addAttribute("username", email);
        model.addAttribute("projectId", projectId);

        return "user/user-viewprojecttasks";
    }
}
