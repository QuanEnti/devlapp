package com.devcollab.controller.view;

import com.devcollab.service.core.ProjectService;
import com.devcollab.service.feature.MessageService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.domain.User;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/user/view")
@RequiredArgsConstructor
public class UserViewController {

    private final MessageService messageService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final UserService userService;

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
    public String userDashboardPage() {
        return "user/user-dashboard";
    }

    // ➕ Create Project Page
    @GetMapping("/create-project")
    public String createProjectPage() {
        return "user/user-createproject";
    }

    // 📋 Xem toàn bộ project của user
    @GetMapping("/view-all-projects")
    public String viewAllProjects(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);
        model.addAttribute("projects", projectService.getProjectsByUsername(email));
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
}
