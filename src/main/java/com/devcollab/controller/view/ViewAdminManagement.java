package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewAdminManagement {

    @GetMapping("/admin/manage")
    public String showUserManagement() {
        return "admin/user-management";
    }

    @GetMapping("/admin/project-reports")
    public String projectReportsPage() {
        return "admin/project-report.html";
    }

    @GetMapping("/admin/user-reports")
    public String showUserReports() {
        return "admin/user-report";  // maps to templates/admin/user-report.html
    }
    @GetMapping("/admin/dashboard")
    public String showDashboard() {
        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/user-logs")
    public String showUserLogs() {
        return "admin/user-log";
    }

}
