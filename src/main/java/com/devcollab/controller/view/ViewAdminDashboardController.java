package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewAdminDashboardController {
    @GetMapping("/admin/dashboard")
    public String showDashboard() {
        return "admin/admin-dashboard";
    }
}

