package com.devcollab.controller.rest;

import com.devcollab.dto.UserDto;
import com.devcollab.domain.Project;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pm/invite")
public class InviteUserRestController {

    private final ProjectRepository projectRepo;
    private final UserRepository userRepo;
    private final ProjectMemberRepository projectMemberRepo;

    public InviteUserRestController(ProjectRepository projectRepo, UserRepository userRepo, ProjectMemberRepository projectMemberRepo) {
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.projectMemberRepo = projectMemberRepo;
    }

    // --- 1️⃣ Get list of all projects (for dropdown)
    @GetMapping("/projects")
    public List<Map<String, Object>> getAllProjects() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Project p : projectRepo.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", p.getProjectId());
            map.put("name", p.getName());
            list.add(map);
        }
        return list;
    }

    @GetMapping("/project/{projectId}")
    public Map<String, Object> getProjectDetails(@PathVariable Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<UserDto> members = projectMemberRepo.findMembersByProjectId(projectId);

        return Map.of(
                "projectId", project.getProjectId(),
                "name", project.getName(),
                "description", project.getDescription(),
                "startDate", project.getStartDate(),
                "endDate", project.getDueDate(),
                "members", members
        );
    }

    // --- 2️⃣ Find user by email (for “Find” button)
    @GetMapping("/find-user")
    public UserDto findUserByEmail(@RequestParam String email) {
        return userRepo.findByEmail(email)
                .map(UserDto::fromEntity)
                .orElse(null);
    }

    // --- 3️⃣ Invite user to selected project
    @PostMapping("/invite")
    public Map<String, Object> inviteUser(@RequestParam Long projectId, @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();

        // Check if user already member
        boolean exists = projectMemberRepo.existsByProject_ProjectIdAndUser_UserId(projectId, userId);
        if (exists) {
            response.put("status", "exists");
            response.put("message", "User is already a member of this project.");
            return response;
        }

        // Add new member
        projectMemberRepo.addMember(projectId, userId, "Member");
        response.put("status", "success");
        response.put("message", "User invited successfully.");
        return response;
    }
    @GetMapping("/related-projects/{userId}")
    public List<Map<String, Object>> getRelatedProjects(@PathVariable Long userId) {
        return projectMemberRepo.findProjectsByUserId(userId)
                .stream()
                .map(p -> {
                    Map<String, Object> projectMap = new HashMap<>();
                    projectMap.put("projectId", p.getProjectId());
                    projectMap.put("name", p.getName());
                    projectMap.put("thumbnailUrl", p.getStatus() != null ? p.getStatus() : "");
                    return projectMap;
                })
                .toList();
    }

}
