package com.devcollab.controller.rest;

import com.devcollab.dto.response.ProjectSearchResponseDTO;
import com.devcollab.service.core.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final ProjectService projectService;

    // Endpoint: /api/search?query=keyword
    @GetMapping("/search")
    public List<ProjectSearchResponseDTO> searchProjects(@RequestParam("query") String keyword) {
        return projectService.searchProjectsByKeyword(keyword);
    }
}
