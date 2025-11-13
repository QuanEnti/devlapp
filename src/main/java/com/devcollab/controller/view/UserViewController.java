package com.devcollab.controller.view;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.Task;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.UserTaskViewDTO;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.feature.MessageService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.domain.User;
import com.devcollab.service.core.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/user/view")
@RequiredArgsConstructor
public class UserViewController {

    private final MessageService messageService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final TaskService taskService;

    /**
     * ✅ Thêm user + unreadNotifications cho MỌI VIEW
     */
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

    // 📌 Hàm dùng lại để lấy email từ Authentication
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }

    // 🏠 Dashboard
    @GetMapping("/dashboard")
    public String viewHome(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        List<Project> activeProjects = projectService.getProjectsByUser(user.getUserId());
        List<Task> myTasks = taskService.getTasksByUser(user);
        List<Task> upcoming = taskService.findUpcomingDeadlines(user.getUserId());

        model.addAttribute("user", user);
        model.addAttribute("activeProjects", activeProjects);
        model.addAttribute("myTasks", myTasks);
        model.addAttribute("upcoming", upcoming);
        return "user/home";
    }


    // ➕ Create Project Page
    @GetMapping("/create-project")
    public String createProjectPage(Model model, Authentication auth) {
        boolean isPremium = false;
        System.out.println("=== DEBUG CREATE PROJECT ===");

        if (auth != null && auth.isAuthenticated()) {
            String email = getEmailFromAuthentication(auth);
            System.out.println("Authenticated user: " + email);

            User user = userService.getByEmail(email).orElse(null);
            System.out.println("User found: " + (user != null));

            if (user != null) {
                isPremium = user.isPremium( );
            } else {
                System.out.println("❌ User not found in database");
            }
        } else {
            System.out.println("❌ User not authenticated");
        }
        model.addAttribute("isPremium", isPremium);
        return "user/user-createproject";
    }
    @GetMapping("/view-all-projects")
    public String viewAllProjects(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  Authentication auth,
                                  @RequestParam(defaultValue = "all") String role) {

        String email = getEmailFromAuthentication(auth);
        User currentUser = userService.getByEmail(email).orElseThrow();

        Pageable pageable = PageRequest.of(page, 9);
        Page<ProjectMember> memberPage = projectService.getProjectsByUserSorted(currentUser, role, pageable);

        // ✅ Just extract the Project entity, no role injection
        List<Project> projectList = memberPage.getContent()
                .stream()
                .map(ProjectMember::getProject)
                .toList();

        model.addAttribute("projects", projectList);
        model.addAttribute("page", memberPage);
        model.addAttribute("role", role);

        return "user/user-viewallprojects";
    }


    // ✉️ Danh sách lời mời
    @GetMapping("/view-invitation")
    public String userViewInvitationPage() {
        return "user/user-viewinvitation";
    }

    // 📧 Tin nhắn theo từng project
    @GetMapping("/message")
    public String userMessagePage(
            @RequestParam(value = "projectId", required = false) Long projectId,
            Model model,
            Authentication auth) {

        String email = getEmailFromAuthentication(auth);
        var projects = projectService.getProjectsByUsername(email);
        model.addAttribute("projects", projects);

        if (projectId != null) {
            model.addAttribute("messages", messageService.getMessagesByProjectId(projectId));
            model.addAttribute("projectId", projectId);
        }

        return "user/user-message";
    }

    // 🧑‍💼 Hồ sơ người dùng
    @GetMapping("/profile")
    public String userProfilePage(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);

        User user = userService.getByEmail(email).orElse(null);
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User tempUser = new User();
            tempUser.setEmail(email);
            tempUser.setName("Unknown User");
            model.addAttribute("user", tempUser);
        }

        return "user/user-profile";
    }

    @GetMapping("/tasks")
    public String userTasksPage(Model model,
                                Authentication auth,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String sortBy,
                                @RequestParam(required = false) String status) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/view/login";

        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        if (user == null) return "redirect:/view/login";

        Page<Task> taskPage = taskService.getUserTasksPaged(user, sortBy, page, size, status);

        // map to your lightweight DTO (or keep entity if your view is safe)
        Page<UserTaskViewDTO> dtoPage = taskPage.map(UserTaskViewDTO::fromEntity);

        model.addAttribute("tasks", dtoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dtoPage.getTotalPages());
        model.addAttribute("totalItems", dtoPage.getTotalElements());
        model.addAttribute("sortBy", (sortBy == null || sortBy.isBlank()) ? "deadline" : sortBy);
        model.addAttribute("status", (status == null || status.isBlank()) ? "ALL" : status);

        return "user/user-task";
    }
}
