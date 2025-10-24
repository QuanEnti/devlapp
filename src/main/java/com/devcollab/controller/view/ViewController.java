package com.devcollab.controller.view;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import com.devcollab.service.core.ProjectService;


@Controller
@RequestMapping("/view")
public class ViewController {
    @GetMapping({ "/", "/home" })
    public String homePage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("loggedIn", true);
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("loggedIn", false);
        }
        return "home";
    }

    @GetMapping("/signin")
    public String signinPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/view/home";
        }
        return "auth/signin";
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/view/home";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/view/home";
        }
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "auth/reset-password";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage() {
        return "auth/verify-otp";
    }

    @GetMapping("/password-reset-success")
    public String passwordResetSuccessPage() {
        return "auth/password-reset-success";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "user/user-dashboard";
    } 
}

