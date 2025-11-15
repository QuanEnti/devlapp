package com.devcollab.controller.view;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.service.core.UserService;
import com.devcollab.service.system.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private final UserService userService;
    private final NotificationService notificationService;
    private final ProjectMemberRepository projectMemberRepository;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return;

        String email = (auth.getPrincipal() instanceof OAuth2User oAuth2User)
                ? (String) oAuth2User.getAttributes().get("email")
                : auth.getName();

        if (email == null)
            return;

        userService.getByEmail(email).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("unreadNotifications", notificationService.countUnread(email));

            // ✅ Kiểm tra xem user có phải là PM không (role hệ thống hoặc role trong project)
            boolean hasSystemPmRole = user.getRoles() != null
                    && user.getRoles().stream().anyMatch(role -> "ROLE_PM".equals(role.getName()));
            boolean hasProjectPmRole = projectMemberRepository
                    .existsByUserEmailAndRoleInProjectIn(email, java.util.List.of("PM", "ADMIN"));
            boolean isProjectManager = hasSystemPmRole || hasProjectPmRole;

            model.addAttribute("isProjectManager", isProjectManager);
            log.debug("✅ Loaded global user: {} ({} notifications, isPM: {})", email,
                    notificationService.countUnread(email), isProjectManager);
        });
    }
}
