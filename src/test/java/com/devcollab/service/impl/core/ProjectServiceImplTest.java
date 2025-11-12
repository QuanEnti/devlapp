package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectAuthorizationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectAuthorizationService authz;
    @Mock private AppEventService appEventService;
    @Mock private ActivityService activityService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ProjectServiceImpl service;

    private User creator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = new User();
        creator.setUserId(1L);
        creator.setEmail("pm@devcollab.com");
    }

    // 🟢 SUCCESS CASE
    @Test
    void createProject_Success() {
        Project project = new Project();
        project.setName("DevCollab");
        project.setStartDate(LocalDate.of(2025, 11, 10));
        project.setDueDate(LocalDate.of(2025, 11, 15));

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setProjectId(100L);
            return saved;
        });

        Project result = service.createProject(project, 1L);

        assertNotNull(result);
        assertEquals("DevCollab", result.getName());
        assertEquals("Active", result.getStatus());
        assertEquals("Normal", result.getPriority());
        assertEquals("private", result.getVisibility());
        verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
        verify(boardColumnRepository, times(4)).save(any(BoardColumn.class));
        verify(appEventService, times(1)).publishProjectCreated(result);
    }

    // 🔴 ERROR: Project is null
    @Test
    void createProject_NullProject_ThrowsException() {
        assertThrows(BadRequestException.class, () -> service.createProject(null, 1L));
    }

    // 🔴 ERROR: Empty name
    @Test
    void createProject_EmptyName_ThrowsException() {
        Project project = new Project();
        project.setName("");
        assertThrows(BadRequestException.class, () -> service.createProject(project, 1L));
    }

    // 🔴 ERROR: Invalid date range
    @Test
    void createProject_InvalidDateRange_ThrowsException() {
        Project project = new Project();
        project.setName("Invalid");
        project.setStartDate(LocalDate.of(2025, 11, 10));
        project.setDueDate(LocalDate.of(2025, 11, 1));

        assertThrows(BadRequestException.class, () -> service.createProject(project, 1L));
    }

    // 🔴 ERROR: Creator not found
    @Test
    void createProject_CreatorNotFound_ThrowsException() {
        Project project = new Project();
        project.setName("Project X");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createProject(project, 1L));
    }
}
