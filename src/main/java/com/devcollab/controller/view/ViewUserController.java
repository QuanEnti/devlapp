package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewUserController {

    @GetMapping("/admin/manage")
    public String showUserManagement() {
        return "admin/user-management"; // loads templates/user-management.html
    }
}
