package com.devcollab.controller.view;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/view/pm")
@RequiredArgsConstructor
public class ViewPmController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final NotificationService notificationService;

    @GetMapping("/project/{id}/dashboard")
    public String dashboard(@PathVariable("id") Long id, Model model) {
        Project project = projectService.getByIdWithMembers(id);
        List<ProjectMember> members = project.getMembers();

        model.addAttribute("project", project);
        model.addAttribute("members", members);
        model.addAttribute("memberCount", members.size());
        model.addAttribute("projectId", id);

        System.out.println(" Loaded dashboard for project " + id + ": " + project.getName());
        return "project/overview-project";
    }
    
    @GetMapping("/project/board")
    public String projectBoard(@RequestParam("projectId") Long id, Model model) {
        model.addAttribute("projectId", id);
        return "project/task-view";
    }
    @GetMapping("/project/detail")
    public String viewProjectDetail(@RequestParam("projectId") Long projectId, Model model) {
        Project project = projectService.getById(projectId);
        if (project == null) {
            return "redirect:/user/view/dashboard";
        }

        model.addAttribute("project", project);
        model.addAttribute("progress", projectService.getProgress(projectId));
        model.addAttribute("metrics", projectService.getMetrics(projectId));
        model.addAttribute("notifications", notificationService.findRecentByProject(projectId));
        return "pm/project-detail.html"; // your Thymeleaf template name
    }


}
