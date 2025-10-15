package com.devcollab.controller.rest;

import com.devcollab.dto.TaskDTO;
import com.devcollab.service.impl.core.TaskServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskRestController {

    private final TaskServiceImpl taskService;

    @GetMapping("/project/{projectId}/member")
    public List<TaskDTO> getTasksForMember(
            @PathVariable Long projectId,
            Authentication authentication) {

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập trước");
        }

        String email;
        Object principal = authentication.getPrincipal();

        if (principal instanceof DefaultOAuth2User oauthUser) {
            email = oauthUser.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return taskService.getTasksByProjectAndMember(projectId, email)
                .stream()
                .map(TaskDTO::fromEntity)
                .toList();
    }
}
