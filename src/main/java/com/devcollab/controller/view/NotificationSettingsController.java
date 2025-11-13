package com.devcollab.controller.view;

import com.devcollab.domain.User;
import com.devcollab.domain.UserSettings;
import com.devcollab.service.system.AuthService;
import com.devcollab.service.system.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/settings/notifications")
public class NotificationSettingsController {

    private final AuthService authService;
    private final UserSettingsService userSettingsService;

    @GetMapping
    public String view(Model model, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            log.error(" User not authenticated.");
            return "redirect:/view/signin";
        }

        User user = authService.getCurrentUserEntity(auth);
        if (user == null) {
            log.error(" Failed to get current user.");
            return "redirect:/view/signin";
        }

        UserSettings settings = userSettingsService.getOrDefault(user);
        if (settings == null) {
            log.warn("User settings not found, using default settings.");
            settings = new UserSettings();
        }

        model.addAttribute("settings", settings);
        return "settings/notification-settings";
    }
}
