package com.devcollab.controller.view;

import com.devcollab.service.core.UserService;
import com.devcollab.service.system.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view/user")
public class ViewUserProfileController {

    private final UserService userService;
    private final NotificationService notificationService;
    public ViewUserProfileController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }
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
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public String viewUserProfile(@PathVariable Long userId) {
        return "user/profile";
    }
}
