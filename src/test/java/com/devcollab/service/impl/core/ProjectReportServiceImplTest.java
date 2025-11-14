package com.devcollab.service.impl.core;

import com.devcollab.domain.*;
import com.devcollab.dto.ProjectReportDto;
import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.*;
import com.devcollab.service.system.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectReportServiceImplTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectReportRepository reportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ProjectReportServiceImpl projectReportService;

    private User testReporter;
    private User testOwner;
    private User testAdmin;
    private Project testProject;
    private ProjectReport testReport;
    private ProjectMember testMember;
    private ReportRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        testOwner = new User();
        testOwner.setUserId(1L);
        testOwner.setEmail("owner@example.com");
        testOwner.setName("Owner User");

        testReporter = new User();
        testReporter.setUserId(2L);
        testReporter.setEmail("reporter@example.com");
        testReporter.setName("Reporter User");

        testAdmin = new User();
        testAdmin.setUserId(3L);
        testAdmin.setEmail("admin@example.com");
        testAdmin.setName("Admin User");

        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");
        testProject.setCreatedBy(testOwner);

        testReport = new ProjectReport();
        testReport.setId(1L);
        testReport.setReporter(testReporter);
        testReport.setProject(testProject);
        testReport.setReason("Inappropriate content");
        testReport.setDetails("Project contains inappropriate content");
        testReport.setStatus("pending");
        testReport.setCreatedAt(Instant.now());

        testMember = new ProjectMember();
        testMember.setUser(testOwner);
        testMember.setProject(testProject);

        testRequestDTO = new ReportRequestDTO();
        testRequestDTO.setReportedId(1L);
        testRequestDTO.setReason("Inappropriate content");
        testRequestDTO.setDetails("Project contains inappropriate content");
        testRequestDTO.setProofUrl("http://example.com/proof.jpg");
    }

    @Test
    void testCreateProjectReport_Success() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testReporter));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(reportRepository.save(any(ProjectReport.class))).thenReturn(testReport);

        // When
        projectReportService.createProjectReport(testRequestDTO, "reporter@example.com");

        // Then
        verify(userRepository).findByEmail("reporter@example.com");
        verify(projectRepository).findById(1L);
        verify(reportRepository).save(any(ProjectReport.class));
    }

    @Test
    void testCreateProjectReport_ReporterNotFound() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectReportService.createProjectReport(testRequestDTO, "reporter@example.com");
        });
    }

    @Test
    void testCreateProjectReport_ProjectNotFound() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testReporter));
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectReportService.createProjectReport(testRequestDTO, "reporter@example.com");
        });
    }

    @Test
    void testCreateProjectReport_SelfReport() {
        // Given
        testRequestDTO.setReportedId(1L);
        testProject.setCreatedBy(testReporter);
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testReporter));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            projectReportService.createProjectReport(testRequestDTO, "reporter@example.com");
        });
    }

    @Test
    void testGetAllReports_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectReport> page = new PageImpl<>(List.of(testReport));
        when(reportRepository.findAll(pageable)).thenReturn(page);

        // When
        Map<String, Object> result = projectReportService.getAllReports(0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        assertTrue(result.containsKey("currentPage"));
        assertTrue(result.containsKey("totalItems"));
        assertTrue(result.containsKey("totalPages"));
        verify(reportRepository).findAll(pageable);
    }

    @Test
    void testUpdateReport_Success() {
        // Given
        Map<String, String> body = new HashMap<>();
        body.put("status", "reviewed");
        body.put("actionTaken", "Warning issued");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(ProjectReport.class))).thenReturn(testReport);

        // When
        ProjectReport result = projectReportService.updateReport(1L, body, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Warning issued", testReport.getActionTaken());
        verify(reportRepository).findById(1L);
        verify(reportRepository).save(testReport);
        verify(activityService).logWithActor(eq(3L), eq("ProjectReport"), eq(1L), eq("update"), anyString());
    }

    @Test
    void testWarnOwner_Success() {
        // Given
        Map<String, String> body = new HashMap<>();
        body.put("message", "Project has been warned");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(List.of(testMember));
        when(reportRepository.save(any(ProjectReport.class))).thenReturn(testReport);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        projectReportService.warnOwner(1L, body, testAdmin);

        // Then
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Warning", testReport.getActionTaken());
        verify(projectMemberRepository).findByProject_ProjectId(1L);
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(activityService).logWithActor(eq(3L), eq("ProjectReport"), eq(1L), eq("warn"), anyString());
    }

    @Test
    void testRemoveProject_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(projectMemberRepository.findByProject_ProjectId(1L)).thenReturn(List.of(testMember));
        when(projectRepository.save(any(Project.class))).thenReturn(testProject);
        when(reportRepository.save(any(ProjectReport.class))).thenReturn(testReport);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        projectReportService.removeProject(1L, testAdmin);

        // Then
        assertEquals("Archived", testProject.getStatus());
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Removed", testReport.getActionTaken());
        verify(projectRepository).save(testProject);
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(activityService).logWithActor(eq(3L), eq("ProjectReport"), eq(1L), eq("ban"), anyString());
    }

    @Test
    void testGetReportById_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));

        // When
        ProjectReportDto result = projectReportService.getReportById(1L);

        // Then
        assertNotNull(result);
        verify(reportRepository).findById(1L);
    }

    @Test
    void testGetReportById_NotFound() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            projectReportService.getReportById(1L);
        });
    }

    @Test
    void testCountViolatedProjects() {
        // Given
        when(reportRepository.countByStatus("pending")).thenReturn(5L);
        when(reportRepository.countByStatus("reviewed")).thenReturn(10L);

        // When
        long result = projectReportService.countViolatedProjects();

        // Then
        assertEquals(15L, result);
        verify(reportRepository).countByStatus("pending");
        verify(reportRepository).countByStatus("reviewed");
    }
}

