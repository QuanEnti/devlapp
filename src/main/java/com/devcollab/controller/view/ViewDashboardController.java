    package com.devcollab.controller.view;

    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RequestMapping;

    @Controller
    @RequestMapping("/view/pm")
    public class ViewDashboardController {

        @GetMapping("/dashboard")
        @PreAuthorize("hasAnyRole('PM','ADMIN')")
        public String pmDashboard() {
            return "project/overview-project";
        }

        @GetMapping("/projects")
        @PreAuthorize("hasAnyRole('PM','ADMIN')")
        public String viewAllProjects() {
            return "project/projects-list";
        }
        
        @GetMapping("/teams")
        @PreAuthorize("hasAnyRole('PM','ADMIN')")
        public String viewTeams() {
            return "project/teams-list";
        }
        
        @GetMapping("/settings")
        @PreAuthorize("hasAnyRole('PM','ADMIN')")
        public String viewSettings() {
            return "project/settings";
        }
        
        @GetMapping("/project")
        @PreAuthorize("hasAnyRole('PM','ADMIN')")
        public String viewSingleProject() {
            return "project/project-detail";
        }

    }
