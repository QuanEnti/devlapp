package com.devcollab.service.impl.system;

import com.devcollab.domain.ProjectTarget;
import com.devcollab.domain.User;
import com.devcollab.dto.response.ProjectPerformanceDTO;
import com.devcollab.dto.response.ProjectSummaryDTO;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.service.impl.core.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectTargetServiceImpl projectTargetService;

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
    }

    @Test
    void testGetProjectSummary_Week() {
        // Given
        List<Object[]> stats = Arrays.asList(
                new Object[]{"ACTIVE", 5L},
                new Object[]{"COMPLETED", 3L}
        );
        when(projectRepository.countProjectsByStatusSince(any(LocalDateTime.class))).thenReturn(stats);

        // When
        ProjectSummaryDTO result = dashboardService.getProjectSummary("week");

        // Then
        assertNotNull(result);
        assertEquals(8L, result.getTotal());
        assertEquals(5L, result.getActive());
        assertEquals(3L, result.getCompleted());
        verify(projectRepository).countProjectsByStatusSince(any(LocalDateTime.class));
    }

    @Test
    void testGetProjectSummary_Month() {
        // Given
        List<Object[]> stats = Arrays.asList(
                new Object[]{"ACTIVE", 10L},
                new Object[]{"IN_PROGRESS", 5L}
        );
        when(projectRepository.countProjectsByStatusSince(any(LocalDateTime.class))).thenReturn(stats);

        // When
        ProjectSummaryDTO result = dashboardService.getProjectSummary("month");

        // Then
        assertNotNull(result);
        assertEquals(15L, result.getTotal());
        assertEquals(10L, result.getActive());
        assertEquals(5L, result.getInProgress());
    }

    @Test
    void testGetProjectSummary_Year() {
        // Given
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"COMPLETED", 20L});
        when(projectRepository.countProjectsByStatusSince(any(LocalDateTime.class))).thenReturn(stats);

        // When
        ProjectSummaryDTO result = dashboardService.getProjectSummary("year");

        // Then
        assertNotNull(result);
        assertEquals(20L, result.getTotal());
        assertEquals(20L, result.getCompleted());
    }

    @Test
    void testGetProjectSummary_Default() {
        // Given
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[]{"ACTIVE", 15L});
        when(projectRepository.countProjectsByStatusSince(any(LocalDateTime.class))).thenReturn(stats);

        // When
        ProjectSummaryDTO result = dashboardService.getProjectSummary("invalid");

        // Then
        assertNotNull(result);
        assertEquals(15L, result.getTotal());
    }

    @Test
    void testGetProjectPerformance_Success() {
        // Given
        setupSecurityContext();
        List<Object[]> results = Arrays.asList(
                new Object[]{"January", 5L},
                new Object[]{"February", 8L}
        );
        List<ProjectTarget> targets = Arrays.asList(
                createTarget(1, 10),
                createTarget(2, 12)
        );
        when(projectRepository.countCompletedProjectsSince(any(LocalDateTime.class))).thenReturn(results);
        when(projectTargetService.getTargetsByYearAndPm(anyInt(), eq(1L))).thenReturn(targets);
        when(userService.getByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        ProjectPerformanceDTO result = dashboardService.getProjectPerformance("month");

        // Then
        assertNotNull(result);
        assertNotNull(result.getLabels());
        assertNotNull(result.getAchieved());
        assertNotNull(result.getTarget());
        verify(projectRepository).countCompletedProjectsSince(any(LocalDateTime.class));
        verify(projectTargetService).getTargetsByYearAndPm(anyInt(), eq(1L));
    }

    @Test
    void testGetProjectPerformance_Week() {
        // Given
        setupSecurityContext();
        List<Object[]> emptyResults = new ArrayList<>();
        when(projectRepository.countCompletedProjectsSince(any(LocalDateTime.class))).thenReturn(emptyResults);
        when(projectTargetService.getTargetsByYearAndPm(anyInt(), eq(1L))).thenReturn(new ArrayList<>());
        when(userService.getByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        ProjectPerformanceDTO result = dashboardService.getProjectPerformance("week");

        // Then
        assertNotNull(result);
    }

    @Test
    void testGetProjectPerformance_Year() {
        // Given
        setupSecurityContext();
        List<Object[]> emptyResults = new ArrayList<>();
        when(projectRepository.countCompletedProjectsSince(any(LocalDateTime.class))).thenReturn(emptyResults);
        when(projectTargetService.getTargetsByYearAndPm(anyInt(), eq(1L))).thenReturn(new ArrayList<>());
        when(userService.getByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        ProjectPerformanceDTO result = dashboardService.getProjectPerformance("year");

        // Then
        assertNotNull(result);
    }

    private ProjectTarget createTarget(int month, int targetCount) {
        return ProjectTarget.builder()
                .month(month)
                .year(2024)
                .targetCount(targetCount)
                .createdBy(1L)
                .build();
    }

    private void setupSecurityContext() {
        Authentication auth = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(auth.getName()).thenReturn("test@example.com");
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }
}

