package com.devcollab.controller.view;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view")
public class TimelineController {

    @GetMapping("/timeline")
    public String timelineView(Model model, Authentication auth) {
        // Add user and notifications via @ModelAttribute if needed
        return "timeline";
    }
}

