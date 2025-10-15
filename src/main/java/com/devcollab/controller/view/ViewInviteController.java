package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewInviteController {

    @GetMapping("/pm/invite")
    public String showInviteUserPage() {
        // Renders the page: src/main/resources/templates/pm/invite-user.html
        return "pm/invite-user";
    }
}
