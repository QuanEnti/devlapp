package com.devcollab.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewUserWorkLog {
    @GetMapping("/user/worklog")
    public String showUserWorkLog() {
        return "user/user-worklog"; // loads templates/user-worklog.html
    }
}
