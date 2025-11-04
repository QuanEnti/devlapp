package com.devcollab.controller.view;

import com.devcollab.dto.UserReportDto;
import com.devcollab.service.core.UserReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ViewReportController {

    private final UserReportService userReportService;

    @GetMapping("/view/report/{id}")
    public String showReportDetail(@PathVariable Long id, Model model) {
        UserReportDto report = userReportService.getReportById(id);
        model.addAttribute("report", report);
        return "report/report-detail"; // thymeleaf page
    }
}
