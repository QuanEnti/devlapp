package com.devcollab.controller.view;

import com.devcollab.service.core.ProjectService;
import com.devcollab.service.feature.MessageService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.domain.User;
import com.devcollab.service.core.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
     * ✅ Phương thức dùng chung cho toàn bộ controller:
     * Tự động thêm username + unreadNotifications vào model cho mọi view.
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            String email = auth.getName();
            User user = userService.getByEmail(email).orElse(null);
            model.addAttribute("user", user);

            int unreadCount = notificationService.countUnread(email);
            model.addAttribute("unreadNotifications", unreadCount);
        }
    }

    // 🏠 Trang Dashboard
    @GetMapping("/dashboard")
    public String userDashboardPage() {
        return "user/user-dashboard";
    }

    // ➕ Trang tạo Project mới
    @GetMapping("/create-project")
    public String createProjectPage() {
        return "user/user-createproject";
    }

    // 📋 Trang xem tất cả Project của user
    @GetMapping("/view-all-projects")
    public String viewAllProjects(Model model, Authentication auth) {
        String username = auth.getName();
        model.addAttribute("projects", projectService.getProjectsByUsername(username));
        return "user/user-viewallprojects";
    }

    // ✉️ Trang xem danh sách lời mời
    @GetMapping("/view-invitation")
    public String userViewInvitationPage() {
        return "user/user-viewinvitation";
    }

    // 📧 Trang xem tin nhắn
    @GetMapping("/message")
    public String userMessagePage(
            @RequestParam(value = "projectId", required = false) Long projectId,
            Model model,
            Authentication auth) {

        String username = auth.getName();
        var projects = projectService.getProjectsByUsername(username);
        model.addAttribute("projects", projects);

        if (projectId != null) {
            var messages = messageService.getMessagesByProjectId(projectId);
            model.addAttribute("messages", messages);
            model.addAttribute("projectId", projectId);
        }

        return "user/user-message";
    }

    // 🧑‍💼 ✅ Trang hồ sơ người dùng
    @GetMapping("/profile")
    public String userProfilePage(Model model, Authentication auth) {
        String email = auth.getName();

        // ✅ Lấy User entity đầy đủ từ DB
        User user = userService.getByEmail(email).orElse(null);

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            // fallback: user không tồn tại (hiếm khi xảy ra)
            User tempUser = new User();
            tempUser.setEmail(email);
            tempUser.setName("Unknown User");
            model.addAttribute("user", tempUser);
        }

        return "user/user-profile";
    }

}
