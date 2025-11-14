package com.devcollab.service.impl.core;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.*;
import com.devcollab.dto.MemberPerformanceDTO;
import com.devcollab.dto.TaskDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import com.devcollab.service.system.ProjectAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private TaskFollowerRepository taskFollowerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Project testProject;
    private BoardColumn testColumn;
    private Task testTask;
    private User testUser;
    private User testCreator;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testColumn = new BoardColumn();
        testColumn.setColumnId(1L);
        testColumn.setName("To-do");
        testColumn.setProject(testProject);
        testColumn.setOrderIndex(1);

        testCreator = new User();
        testCreator.setUserId(1L);
        testCreator.setEmail("creator@example.com");
        testCreator.setName("Creator User");

        testUser = new User();
        testUser.setUserId(2L);
        testUser.setEmail("user@example.com");
        testUser.setName("Test User");

        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescriptionMd("Test Description");
        testTask.setStatus("OPEN");
        testTask.setPriority("MEDIUM");
        testTask.setProject(testProject);
        testTask.setColumn(testColumn);
        testTask.setCreatedBy(testCreator);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());
        testTask.setArchived(false);
        testTask.setFollowers(new ArrayList<>());
    }

    @Test
    void testCreateTaskFromDTO_Success() {
        // Given
        TaskDTO dto = new TaskDTO();
        dto.setTitle("New Task");
        dto.setDescriptionMd("Task Description");
        dto.setPriority("HIGH");
        dto.setColumnId(1L);
        when(boardColumnRepository.findById(1L)).thenReturn(Optional.of(testColumn));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        Task result = taskService.createTaskFromDTO(dto, 1L);

        // Then
        assertNotNull(result);
        verify(boardColumnRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
        verify(activityService).log(eq("TASK"), eq(testTask.getTaskId()), eq("CREATE_TASK"), anyString(), any());
    }

    @Test
    void testCreateTaskFromDTO_NullDTO() {
        // When & Then
        assertThrows(BadRequestException.class, () -> {
            taskService.createTaskFromDTO(null, 1L);
        });
    }

    @Test
    void testCreateTaskFromDTO_ColumnNotFound() {
        // Given
        TaskDTO dto = new TaskDTO();
        dto.setTitle("New Task");
        dto.setColumnId(1L);
        when(boardColumnRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            taskService.createTaskFromDTO(dto, 1L);
        });
    }

    @Test
    void testQuickCreate_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("creator@example.com", 1L);
            when(boardColumnRepository.findById(1L)).thenReturn(Optional.of(testColumn));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testCreator));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);
            doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

            // When
            Task result = taskService.quickCreate("Quick Task", 1L, 1L, 1L);

            // Then
            assertNotNull(result);
            verify(taskRepository).save(any(Task.class));
        }
    }

    @Test
    void testQuickCreate_BlankTitle() {
        // When & Then
        assertThrows(BadRequestException.class, () -> {
            taskService.quickCreate("   ", 1L, 1L, 1L);
        });
    }

    @Test
    void testUpdateTask_Success() {
        // Given
        Task patch = new Task();
        patch.setTitle("Updated Task");
        patch.setDescriptionMd("Updated Description");
        patch.setPriority("HIGH");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        Task result = taskService.updateTask(1L, patch);

        // Then
        assertNotNull(result);
        assertEquals("Updated Task", testTask.getTitle());
        assertEquals("Updated Description", testTask.getDescriptionMd());
        assertEquals("HIGH", testTask.getPriority());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testUpdateTask_NotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            taskService.updateTask(1L, new Task());
        });
    }

    @Test
    void testDeleteTask_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("creator@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            doNothing().when(taskRepository).delete(any(Task.class));
            doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

            // When
            taskService.deleteTask(1L, testCreator);

            // Then
            verify(taskRepository).delete(testTask);
            verify(activityService).log(eq("TASK"), eq(1L), eq("DELETE_TASK"), anyString(), eq(testCreator));
        }
    }

    @Test
    void testDeleteTask_AsCreator_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("creator@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            doNothing().when(taskRepository).delete(any(Task.class));
            doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

            // When
            taskService.deleteTask(1L, testCreator);

            // Then
            verify(taskRepository).delete(testTask);
        }
    }

    @Test
    void testDeleteTask_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("user@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                taskService.deleteTask(1L, testUser);
            });
        }
    }

    @Test
    void testAssignTask_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        Task result = taskService.assignTask(1L, 2L);

        // Then
        assertNotNull(result);
        assertNotNull(testTask.getAssignee());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testCloseTask_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        Task result = taskService.closeTask(1L);

        // Then
        assertNotNull(result);
        assertEquals("CLOSED", testTask.getStatus());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testReopenTask_Success() {
        // Given
        testTask.setStatus("CLOSED");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        Task result = taskService.reopenTask(1L);

        // Then
        assertNotNull(result);
        assertEquals("OPEN", testTask.getStatus());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testGetTasksByProject_Success() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(taskRepository.findByProject_ProjectIdAndArchivedFalse(1L)).thenReturn(Arrays.asList(testTask));

        // When
        List<TaskDTO> result = taskService.getTasksByProject(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findByProject_ProjectIdAndArchivedFalse(1L);
    }

    @Test
    void testGetTasksByProject_ProjectNotFound() {
        // Given
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            taskService.getTasksByProject(1L);
        });
    }

    @Test
    void testGetById_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        Task result = taskService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testTask, result);
        verify(taskRepository).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            taskService.getById(1L);
        });
    }

    @Test
    void testGetByIdAsDTO_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        TaskDTO result = taskService.getByIdAsDTO(1L);

        // Then
        assertNotNull(result);
        verify(taskRepository).findById(1L);
    }

    @Test
    void testArchiveTask_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("creator@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            // When
            boolean result = taskService.archiveTask(1L, testCreator);

            // Then
            assertTrue(result);
            assertTrue(testTask.isArchived());
            verify(taskRepository).save(testTask);
        }
    }

    @Test
    void testRestoreTask_AsPM_Success() {
        // Given
        testTask.setArchived(true);
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("creator@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(taskRepository.save(any(Task.class))).thenReturn(testTask);

            // When
            boolean result = taskService.restoreTask(1L, testCreator);

            // Then
            assertTrue(result);
            assertFalse(testTask.isArchived());
            verify(taskRepository).save(testTask);
        }
    }

    @Test
    void testMarkComplete_AsPM_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testCreator));
        doNothing().when(projectAuthorizationService).ensurePmOfProject("creator@example.com", 1L);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        TaskDTO result = taskService.markComplete(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals("DONE", testTask.getStatus());
        assertNotNull(testTask.getClosedAt());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testMarkComplete_AsAssignee_Success() {
        // Given
        testTask.setAssignee(testUser);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        doThrow(new AccessDeniedException("Not PM")).when(projectAuthorizationService).ensurePmOfProject("user@example.com", 1L);
        when(taskFollowerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        doNothing().when(activityService).log(anyString(), anyLong(), anyString(), anyString(), any());

        // When
        TaskDTO result = taskService.markComplete(1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals("DONE", testTask.getStatus());
    }

    @Test
    void testMarkComplete_NoPermission() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        doThrow(new AccessDeniedException("Not PM")).when(projectAuthorizationService).ensurePmOfProject("user@example.com", 1L);
        when(taskFollowerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(false);

        // When & Then
        assertThrows(SecurityException.class, () -> {
            taskService.markComplete(1L, 2L);
        });
    }

    @Test
    void testMarkIncomplete_Success() {
        // Given
        testTask.setStatus("DONE");
        doNothing().when(projectAuthorizationService).ensurePmOfProject("creator@example.com", 1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        TaskDTO result = taskService.markIncomplete(1L, 1L, "creator@example.com");

        // Then
        assertNotNull(result);
        assertEquals("OPEN", testTask.getStatus());
        assertNull(testTask.getClosedAt());
        verify(taskRepository).save(testTask);
    }

    @Test
    void testGetTasksByAssignee_Success() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findByAssignee_UserId(2L)).thenReturn(tasks);

        // When
        List<Task> result = taskService.getTasksByAssignee(2L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findByAssignee_UserId(2L);
    }

    @Test
    void testGetPercentDoneByStatus_Success() {
        // Given
        List<Map<String, Object>> raw = new ArrayList<>();
        Map<String, Object> status1 = new HashMap<>();
        status1.put("status", "DONE");
        status1.put("count", 5L);
        Map<String, Object> status2 = new HashMap<>();
        status2.put("status", "OPEN");
        status2.put("count", 5L);
        raw.add(status1);
        raw.add(status2);
        when(taskRepository.countTasksByStatus(1L)).thenReturn(raw);

        // When
        Map<String, Object> result = taskService.getPercentDoneByStatus(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("DONE"));
        assertTrue(result.containsKey("OPEN"));
        assertTrue(result.containsKey("total"));
        verify(taskRepository).countTasksByStatus(1L);
    }

    @Test
    void testGetMemberPerformance_Success() {
        // Given
        Object[] row = new Object[]{
                1L, "Test User", "user@example.com", 10L, 8L, 6L, 2L, 5.5, 20
        };
        List<Object[]> rows = new ArrayList<>();
        rows.add(row);
        when(taskRepository.findMemberPerformanceByProject(1L)).thenReturn(rows);

        // When
        List<MemberPerformanceDTO> result = taskService.getMemberPerformance(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        MemberPerformanceDTO dto = result.get(0);
        assertEquals(1L, dto.getUserId());
        assertEquals(10L, dto.getTotalTasks());
        assertEquals(8L, dto.getCompletedTasks());
        verify(taskRepository).findMemberPerformanceByProject(1L);
    }

    @Test
    void testGetUserTasksPaged_ByStatus_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(Arrays.asList(testTask));
        when(taskRepository.findUserTasksByStatus(testUser, "OPEN", pageable)).thenReturn(page);

        // When
        Page<Task> result = taskService.getUserTasksPaged(testUser, "deadline", 0, 10, "OPEN");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(taskRepository).findUserTasksByStatus(testUser, "OPEN", pageable);
    }

    @Test
    void testGetUserTasksPaged_ByPriority_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(Arrays.asList(testTask));
        when(taskRepository.findUserTasksOrderByPriority(testUser, pageable)).thenReturn(page);

        // When
        Page<Task> result = taskService.getUserTasksPaged(testUser, "priority", 0, 10, "ALL");

        // Then
        assertNotNull(result);
        verify(taskRepository).findUserTasksOrderByPriority(testUser, pageable);
    }

    @Test
    void testFindUpcomingDeadlines_Success() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findTopUpcoming(1L, PageRequest.of(0, 5))).thenReturn(tasks);

        // When
        List<Task> result = taskService.findUpcomingDeadlines(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository).findTopUpcoming(1L, PageRequest.of(0, 5));
    }
}

