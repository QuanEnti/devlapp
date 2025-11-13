package com.devcollab.controller.view;

import com.devcollab.dto.ProjectReportDto;
import com.devcollab.dto.UserReportDto;
import com.devcollab.service.core.ProjectReportService;
import com.devcollab.service.core.UserReportService;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ViewReportController {

    private final UserReportService userReportService;
    private final ProjectReportService projectReportService;
    private final UserService userService;
    private final NotificationService notificationService;
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
    @GetMapping("/view/user-report/{id}")
    public String showUserReport(@PathVariable Long id, Model model) {
        UserReportDto report = userReportService.getReportById(id);
        model.addAttribute("report", report);
        return "report/user-report-detail";
    }

    @GetMapping("/view/project-report/{id}")
    public String showProjectReport(@PathVariable Long id, Model model) {
        ProjectReportDto report = projectReportService.getReportById(id);
        model.addAttribute("report", report);
        return "report/project-report-detail";
    }

}
