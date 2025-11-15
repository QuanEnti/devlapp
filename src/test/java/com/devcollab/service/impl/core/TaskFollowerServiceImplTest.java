package com.devcollab.service.impl.core;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.*;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskFollowerServiceImplTest {

    @Mock
    private TaskFollowerRepository followerRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ActivityService activityService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private TaskFollowerServiceImpl taskFollowerService;

    private Task testTask;
    private User testUser;
    private User testActor;
    private Project testProject;
    private TaskFollower testFollower;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");
        testTask.setProject(testProject);

        testUser = new User();
        testUser.setUserId(2L);
        testUser.setEmail("user@example.com");
        testUser.setName("Test User");

        testActor = new User();
        testActor.setUserId(1L);
        testActor.setEmail("actor@example.com");
        testActor.setName("Actor User");

        testFollower = new TaskFollower();
        testFollower.setTask(testTask);
        testFollower.setUser(testUser);
    }

    @Test
    void testGetFollowersByTask_Success() {
        // Given
        List<TaskFollower> followers = Arrays.asList(testFollower);
        when(followerRepository.findByTask_TaskId(1L)).thenReturn(followers);

        // When
        List<TaskFollowerDTO> result = taskFollowerService.getFollowersByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(followerRepository).findByTask_TaskId(1L);
    }

    @Test
    void testAssignMember_Success() {
        // Given
        setupSecurityContext();
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(projectAuthorizationService);
            doNothing().when(projectAuthorizationService).ensurePmOfProject("actor@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("actor@example.com")).thenReturn(Optional.of(testActor));
            when(followerRepository.saveAndFlush(any(TaskFollower.class))).thenReturn(testFollower);

            // When
            boolean result = taskFollowerService.assignMember(1L, 2L);

            // Then
            assertTrue(result);
            verify(taskRepository).findById(1L);
            verify(followerRepository).existsByTask_TaskIdAndUser_UserId(1L, 2L);
            verify(userRepository).findById(2L);
            verify(followerRepository).saveAndFlush(any(TaskFollower.class));
            verify(activityService).log(anyString(), eq(1L), eq("ADD_MEMBER"), anyString());
        }
    }

    @Test
    void testAssignMember_TaskNotFound() {
        // Given
        setupSecurityContext();
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            taskFollowerService.assignMember(1L, 2L);
        });
    }

    @Test
    void testAssignMember_AlreadyAssigned() {
        // Given
        setupSecurityContext();
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(projectAuthorizationService);
            doNothing().when(projectAuthorizationService).ensurePmOfProject("actor@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(true);
            when(userRepository.findByEmail("actor@example.com")).thenReturn(Optional.of(testActor));

            // When
            boolean result = taskFollowerService.assignMember(1L, 2L);

            // Then
            assertFalse(result);
            verify(followerRepository, never()).saveAndFlush(any());
        }
    }

    @Test
    void testAssignMember_NoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            taskFollowerService.assignMember(1L, 2L);
        });
    }

    @Test
    void testUnassignMember_Success() {
        // Given
        setupSecurityContext();
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(projectAuthorizationService);
            doNothing().when(projectAuthorizationService).ensurePmOfProject("actor@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(true);
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("actor@example.com")).thenReturn(Optional.of(testActor));
            doNothing().when(followerRepository).deleteByTaskAndUser(1L, 2L);

            // When
            boolean result = taskFollowerService.unassignMember(1L, 2L);

            // Then
            assertTrue(result);
            verify(taskRepository).findById(1L);
            verify(followerRepository).existsByTask_TaskIdAndUser_UserId(1L, 2L);
            verify(followerRepository).deleteByTaskAndUser(1L, 2L);
            verify(activityService).log(anyString(), eq(1L), eq("REMOVE_MEMBER"), anyString());
        }
    }

    @Test
    void testUnassignMember_NotAssigned() {
        // Given
        setupSecurityContext();
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(projectAuthorizationService);
            doNothing().when(projectAuthorizationService).ensurePmOfProject("actor@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 2L)).thenReturn(false);
            when(userRepository.findByEmail("actor@example.com")).thenReturn(Optional.of(testActor));

            // When
            boolean result = taskFollowerService.unassignMember(1L, 2L);

            // Then
            assertFalse(result);
            verify(followerRepository, never()).deleteByTaskAndUser(anyLong(), anyLong());
        }
    }

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("actor@example.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}

