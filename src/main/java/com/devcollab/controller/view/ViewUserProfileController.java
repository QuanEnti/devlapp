package com.devcollab.controller.view;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/view/user")
public class ViewUserProfileController {

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public String viewUserProfile(@PathVariable Long userId) {
        return "user/profile";
    }
}
