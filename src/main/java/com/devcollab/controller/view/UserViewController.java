package com.devcollab.controller.view;

import com.devcollab.domain.*;
import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.TaskDTO;
import com.devcollab.dto.TaskStatisticsDTO;
import com.devcollab.dto.UserTaskViewDTO;
import com.devcollab.repository.ProjectMemberRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.ProjectScheduleRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.core.ProjectService;
import com.devcollab.service.core.TaskService;
import com.devcollab.service.feature.MessageService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.core.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/user/view")
@RequiredArgsConstructor
public class UserViewController {

    private final MessageService messageService;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final TaskService taskService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectScheduleRepository projectScheduleRepository;
    private final TaskService taskStatisticsService;

    /**
     * ✅ Thêm user + unreadNotifications cho MỌI VIEW
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return;

        // ✅ Lấy đúng email theo từng trường hợp (Local hoặc Google OAuth2)
        String email = getEmailFromAuthentication(auth);
        if (email == null)
            return;

        final String userEmail = email; // phải là final nếu dùng trong lambda

        userService.getByEmail(userEmail).ifPresent(user -> {
            model.addAttribute("user", user);
            model.addAttribute("unreadNotifications", notificationService.countUnread(userEmail));
        });
    }

    // 📌 Hàm dùng lại để lấy email từ Authentication
    private String getEmailFromAuthentication(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            var attributes = oauthToken.getPrincipal().getAttributes();
            return (String) attributes.get("email");
        }
        return auth.getName(); // Local login
    }

    // 🏠 Dashboard
    @GetMapping("/dashboard")
    public String getDashboard(Model model, Authentication auth) {
        try {
            String email = getEmailFromAuthentication(auth);
            User user = userService.getByEmail(email).orElse(null);
            if (user == null) {
                return "redirect:/view/login";
            }

            // Get user's projects
            List<Project> projects = Collections.emptyList();
            try {
                projects = projectService.getTop5ProjectsByUser(user.getUserId());
                projects = projects.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Error loading projects: " + e.getMessage());
                projects = Collections.emptyList();
            }
            System.out.println("pro: "+projects);
            model.addAttribute("projects", projects);

            // Get task counts - ONLY 3 DATA POINTS
            long openCount = 0;
            long inProgressCount = 0;
            long doneCount = 0;
            long totalTasks = 0;

            try {
                List<Task> userTasks = taskRepository.findAllUserTasks(user);
                totalTasks = userTasks.size();

                openCount = userTasks.stream()
                        .filter(task -> task != null && "OPEN".equals(task.getStatus()))
                        .count();

                inProgressCount = userTasks.stream()
                        .filter(task -> task != null && "IN_PROGRESS".equals(task.getStatus()))
                        .count();

                doneCount = userTasks.stream()
                        .filter(task -> task != null && "DONE".equals(task.getStatus()))
                        .count();

                // Print the 3 data points
                System.out.println("📊 Task Statistics:");
                System.out.println("  - OPEN: " + openCount + " tasks");
                System.out.println("  - IN_PROGRESS: " + inProgressCount + " tasks");
                System.out.println("  - DONE: " + doneCount + " tasks");
                System.out.println("  - Total: " + totalTasks + " tasks");

            } catch (Exception e) {
                System.err.println("Error loading task statistics: " + e.getMessage());
            }
            model.addAttribute("openCount", openCount);
            model.addAttribute("inProgressCount", inProgressCount);
            model.addAttribute("doneCount", doneCount);
            model.addAttribute("totalTasks", totalTasks);

            // Get today's schedule
            List<ProjectSchedule> todaysSchedules = Collections.emptyList();
            try {
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

                todaysSchedules = projects.stream()
                        .filter(Objects::nonNull)
                        .flatMap(project -> {
                            try {
                                return projectScheduleRepository
                                        .findByProject_ProjectIdAndDatetimeBetween(
                                                project.getProjectId(),
                                                startOfDay.toInstant(ZoneOffset.UTC),
                                                endOfDay.toInstant(ZoneOffset.UTC)
                                        ).stream();
                            } catch (Exception e) {
                                System.err.println("Error loading schedule for project " + project.getProjectId() + ": " + e.getMessage());
                                return Stream.empty();
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Error loading today's schedule: " + e.getMessage());
            }
            model.addAttribute("todaysSchedules", todaysSchedules);

            // Get upcoming tasks
            List<Task> upcomingTasks = Collections.emptyList();
            try {
                Pageable topThree = PageRequest.of(0, 3);
                upcomingTasks = taskRepository.findTopUpcoming(user.getUserId(), topThree);
                upcomingTasks = upcomingTasks.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                System.err.println("Error loading upcoming tasks: " + e.getMessage());
            }
            model.addAttribute("upcomingTasks", upcomingTasks);

            // Get performance data
            int janCount = 0, febCount = 0, marCount = 0, aprCount = 0, mayCount = 0, junCount = 0;

            try {
                List<Task> userTasks = taskRepository.findAllUserTasks(user);

                // Count completed tasks for each month
                for (Task task : userTasks) {
                    if (task != null && "DONE".equals(task.getStatus()) && task.getClosedAt() != null) {
                        int month = task.getClosedAt().getMonthValue(); // 1=Jan, 2=Feb, etc.

                        switch (month) {
                            case 1: janCount++; break;
                            case 2: febCount++; break;
                            case 3: marCount++; break;
                            case 4: aprCount++; break;
                            case 5: mayCount++; break;
                            case 6: junCount++; break;
                        }
                    }
                }

                System.out.println("📈 Performance Data:");
                System.out.println("  - Jan: " + janCount + " completed tasks");
                System.out.println("  - Feb: " + febCount + " completed tasks");
                System.out.println("  - Mar: " + marCount + " completed tasks");
                System.out.println("  - Apr: " + aprCount + " completed tasks");
                System.out.println("  - May: " + mayCount + " completed tasks");
                System.out.println("  - Jun: " + junCount + " completed tasks");

            } catch (Exception e) {
                System.err.println("Error loading performance data: " + e.getMessage());
                e.printStackTrace();
            }

            // Add performance counts to model
            model.addAttribute("janCount", janCount);
            model.addAttribute("febCount", febCount);
            model.addAttribute("marCount", marCount);
            model.addAttribute("aprCount", aprCount);
            model.addAttribute("mayCount", mayCount);
            model.addAttribute("junCount", junCount);

            return "user/user-dashboard1";

        } catch (Exception e) {
            System.err.println("Critical error in dashboard: " + e.getMessage());
            return "redirect:/view/login";
        }
    }

    // ➕ Create Project Page
    @GetMapping("/create-project")
    public String createProjectPage(Model model, Authentication auth) {
        boolean isPremium = false;
        System.out.println("=== DEBUG CREATE PROJECT ===");

        if (auth != null && auth.isAuthenticated()) {
            String email = getEmailFromAuthentication(auth);
            System.out.println("Authenticated user: " + email);

            User user = userService.getByEmail(email).orElse(null);
            System.out.println("User found: " + (user != null));

            if (user != null) {
                isPremium = user.isPremium( );
                System.out.println("User is premium: " + isPremium);
            } else {
                System.out.println("❌ User not found in database");
            }
        } else {
            System.out.println("❌ User not authenticated");
        }
        model.addAttribute("isPremium", isPremium);
        return "user/user-createproject";
    }
    @GetMapping("/view-all-projects")
    public String viewAllProjects(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  Authentication auth,
                                  @RequestParam(defaultValue = "all") String role) {

        String email = getEmailFromAuthentication(auth);
        User currentUser = userService.getByEmail(email).orElseThrow();

        Pageable pageable = PageRequest.of(page, 9);
        Page<ProjectMember> memberPage =
                projectService.getProjectsByUserSorted(currentUser, role, pageable);

<<<<<<< HEAD
        // ✅ Just extract the Project entity, no role injection
        List<Project> projectList = memberPage.getContent()
                .stream()
                .map(ProjectMember::getProject)
                .filter(p -> !"Archived".equalsIgnoreCase(p.getStatus()))
                .toList();
=======
        // Build a wrapper list to send to UI
        List<Map<String, Object>> projectCards = new ArrayList<>();
>>>>>>> payment

        for (ProjectMember pm : memberPage.getContent()) {
            Project project = pm.getProject();

            List<MemberDTO> members =
                    projectMemberRepository.findMembersByProject(project.getProjectId());

            Map<String, Object> card = new HashMap<>();
            card.put("project", project);
            card.put("role", pm.getRoleInProject());
            card.put("members", members);

            projectCards.add(card);
        }

        model.addAttribute("cards", projectCards);
        model.addAttribute("role", role);
        model.addAttribute("page", memberPage);

        return "user/user-viewallprojects";
    }



    // ✉️ Danh sách lời mời
    @GetMapping("/view-invitation")
    public String userViewInvitationPage() {
        return "user/user-viewinvitation";
    }

    // 📧 Tin nhắn theo từng project
    @GetMapping("/message")
    public String userMessagePage(
            @RequestParam(value = "projectId", required = false) Long projectId,
            Model model,
            Authentication auth) {

        String email = getEmailFromAuthentication(auth);
        var projects = projectService.getProjectsByUsername(email);
        model.addAttribute("projects", projects);

        if (projectId != null) {
            model.addAttribute("messages", messageService.getMessagesByProjectId(projectId));
            model.addAttribute("projectId", projectId);
        }

        return "user/user-message";
    }

    // 🧑‍💼 Hồ sơ người dùng
    @GetMapping("/profile")
    public String userProfilePage(Model model, Authentication auth) {
        String email = getEmailFromAuthentication(auth);

        User user = userService.getByEmail(email).orElse(null);
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            User tempUser = new User();
            tempUser.setEmail(email);
            tempUser.setName("Unknown User");
            model.addAttribute("user", tempUser);
        }

        return "user/user-profile";
    }

    @GetMapping("/tasks")
    public String userTasksPage(Model model,
                                Authentication auth,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String sortBy,
                                @RequestParam(required = false) String status) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/view/login";

        String email = getEmailFromAuthentication(auth);
        User user = userService.getByEmail(email).orElse(null);
        if (user == null) return "redirect:/view/login";

        Page<Task> taskPage = taskService.getUserTasksPaged(user, sortBy, page, size, status);

        // map to your lightweight DTO (or keep entity if your view is safe)
        Page<UserTaskViewDTO> dtoPage = taskPage.map(UserTaskViewDTO::fromEntity);

        model.addAttribute("tasks", dtoPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dtoPage.getTotalPages());
        model.addAttribute("totalItems", dtoPage.getTotalElements());
        model.addAttribute("sortBy", (sortBy == null || sortBy.isBlank()) ? "deadline" : sortBy);
        model.addAttribute("status", (status == null || status.isBlank()) ? "ALL" : status);

        return "user/user-task";
    }
}
