package com.devcollab.controller.rest;

import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.UserDTO;
import com.devcollab.service.core.UserProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProjectRestController {

    private final UserProjectService userProjectService;

    /** 🔹 Get all projects user joined */
    @GetMapping("/{id}/projects")
    public ResponseEntity<List<ProjectDTO>> getProjects(@PathVariable Long id) {
        List<ProjectDTO> projects = userProjectService.getProjectsByUser(id);
        return ResponseEntity.ok(projects);
    }

    /** 🔹 Get all collaborators ("worked with") */
    @GetMapping("/{id}/worked-with")
    public ResponseEntity<List<UserDTO>> getWorkedWith(@PathVariable Long id) {
        List<UserDTO> collaborators = userProjectService.getWorkedWithUsers(id);
        return ResponseEntity.ok(collaborators);
    }
}
