package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user/view")
public class UserViewController {

    // 🏠 Trang Dashboard của người dùng
    @GetMapping("/dashboard")
    public String userDashboardPage() {
        return "user/user-dashboard"; // => templates/user/user-dashboard.html
    }

    // ➕ Trang tạo Project mới
    @GetMapping("/create-project")
    public String userCreateProjectPage() {
        return "user/user-createproject"; // => templates/user/user-createproject.html
    }

    // 📋 Trang xem tất cả Project của user
    @GetMapping("/view-all-projects")
    public String userViewAllProjectsPage() {
        return "user/user-viewallprojects"; // => templates/user/user-viewallprojects.html
    }

    // ✉️ Trang xem danh sách lời mời (invitation)
    @GetMapping("/view-invitation")
    public String userViewInvitationPage() {
        return "user/user-viewinvitation"; // => templates/user/user-viewinvitation.html
    }
    @GetMapping("/ai-chat")
    public String aiChatPage() {
        return "user/ai-chat";
    }
    @GetMapping("/ai-image")
    public String aiImagePage() {
        return "user/ai-image";
    }

}
