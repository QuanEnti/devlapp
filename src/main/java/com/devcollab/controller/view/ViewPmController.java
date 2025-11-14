package com.devcollab.controller.view;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberPerformanceDTO;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/view/pm")
@RequiredArgsConstructor
public class ViewPmController {

    private final ProjectService projectService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final TaskService taskService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return;

        // ✅ Lấy đúng email theo từng trường hợp (Local hoặc Google OAuth2)
        String email = getEmailFromAuthentication(auth);
        if (email == null)
            return;

        final String userEmail = email; // phải là final nếu dùng trong lambda

        userService.getByEmail(userEmail).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("unreadNotifications", notificationService.countUnread(userEmail));
        });
    }
    @GetMapping("/project/schedule/calendar")
    public String scheduleCalendar(@RequestParam Long projectId, Model model, Authentication auth) {
        Project project = projectService.getById(projectId);
        if (project == null) {
            return "redirect:/user/view/dashboard";
        }

        String email = getEmailFromAuthentication(auth);
        String userRole = projectService.getUserRoleInProjectByEmail(projectId, email);
        System.out.println("User role in project " + projectId + ": " + userRole);
        model.addAttribute("projectId", projectId);
        model.addAttribute("project", project);
        model.addAttribute("userRole", userRole);

        return "pm/schedule-calendar";
    }
    @GetMapping("/project/{id}/dashboard")
    public String dashboard(@PathVariable("id") Long id, Model model) {
        Project checkStatusProject = projectService.getById(id);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + id;
        }

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
        Project checkStatusProject = projectService.getById(id);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + id;
        }

        model.addAttribute("projectId", id);
        return "project/task-view";
    }

    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }

    @GetMapping("/project/detail")
    public String viewProjectDetail(
            @RequestParam("projectId") Long projectId,
            Model model,
            Authentication auth) {
        Project checkStatusProject = projectService.getById(projectId);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + projectId;
        }

        Project project = projectService.getById(projectId);
        if (project == null) {
            return "redirect:/user/view/dashboard";
        }

        // ✅ Get current user's email
        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        // ✅ Reuse existing ProjectService function to get role
        String roleInProject = projectService.getUserRoleInProjectByEmail(projectId, email);

        // ✅ Add role & other data to model
        model.addAttribute("project", project);
        model.addAttribute("roleInProject", roleInProject);
        model.addAttribute("statusBreakdown", taskService.getPercentDoneByStatus(projectId));
        model.addAttribute("metrics", projectService.getMetrics(projectId));
        model.addAttribute("notifications", notificationService.findRecentByProject(projectId));
        model.addAttribute("currentUser", user);
        return "pm/project-detail.html";
    }

    @GetMapping("/project/members")
    public String viewProjectMembers(@RequestParam Long projectId, Model model, Authentication auth) {
        Project checkStatusProject = projectService.getById(projectId);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + projectId;
        }
        String email = getEmailFromAuthentication(auth);
        String roleInProject = projectService.getUserRoleInProjectByEmail(projectId, email);

        model.addAttribute("projectId", projectId);
        model.addAttribute("roleInProject", roleInProject);

        return "pm/project-members";
    }

    @GetMapping("/project/review")
    public String viewProjectReview(@RequestParam Long projectId, Model model) {
        Project checkStatusProject = projectService.getById(projectId);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + projectId;
        }
        model.addAttribute("projectId", projectId);
        return "pm/project-task-review"; // ✅ templates/pm/project-task-review.html
    }

    @GetMapping("/project/performance")
    public String viewPerformance(@RequestParam("projectId") Long projectId, Model model) {
        Project checkStatusProject = projectService.getById(projectId);
        if (checkStatusProject == null || "Archived".equalsIgnoreCase(checkStatusProject.getStatus())) {
            return "redirect:/view/pm/project-archived?projectId=" + projectId;
        }
        Project project = projectService.getById(projectId);
        var performanceList = taskService.getMemberPerformance(projectId);

        // Extract names & scores for Chart.js
        List<String> names = performanceList.stream()
                .map(MemberPerformanceDTO::getName)
                .toList();

        List<Double> scores = performanceList.stream()
                .map(MemberPerformanceDTO::getPerformanceScore)
                .toList();

        model.addAttribute("project", project);
        model.addAttribute("performance", performanceList);
        model.addAttribute("labels", names);
        model.addAttribute("scores", scores);

        return "pm/project-performance.html";
    }

    @GetMapping("/project-archived")
    public String projectArchived(@RequestParam Long projectId, Model model) {
        model.addAttribute("projectId", projectId);
        return "pm/project-archived";
    }

}
