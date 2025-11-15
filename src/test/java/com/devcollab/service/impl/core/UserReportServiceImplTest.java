package com.devcollab.service.impl.core;

import com.devcollab.domain.Notification;
import com.devcollab.domain.User;
import com.devcollab.domain.UserReport;
import com.devcollab.dto.UserReportDto;
import com.devcollab.dto.request.ReportRequestDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.repository.UserReportRepository;
import com.devcollab.repository.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReportServiceImplTest {

    @Mock
    private UserReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private UserReportServiceImpl userReportService;

    private User testReporter;
    private User testReported;
    private User testAdmin;
    private UserReport testReport;
    private ReportRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        testReporter = new User();
        testReporter.setUserId(1L);
        testReporter.setEmail("reporter@example.com");
        testReporter.setName("Reporter User");

        testReported = new User();
        testReported.setUserId(2L);
        testReported.setEmail("reported@example.com");
        testReported.setName("Reported User");

        testAdmin = new User();
        testAdmin.setUserId(3L);
        testAdmin.setEmail("admin@example.com");
        testAdmin.setName("Admin User");

        testReport = new UserReport();
        testReport.setId(1L);
        testReport.setReporter(testReporter);
        testReport.setReported(testReported);
        testReport.setReason("Spam");
        testReport.setDetails("User is spamming");
        testReport.setStatus("pending");
        testReport.setCreatedAt(Instant.now());

        testRequestDTO = new ReportRequestDTO();
        testRequestDTO.setReportedId(2L);
        testRequestDTO.setReason("Spam");
        testRequestDTO.setDetails("User is spamming");
        testRequestDTO.setProofUrl("http://example.com/proof.jpg");
    }

    @Test
    void testCreateUserReport_Success() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testReporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testReported));
        when(reportRepository.save(any(UserReport.class))).thenReturn(testReport);

        // When
        userReportService.createUserReport(testRequestDTO, "reporter@example.com");

        // Then
        verify(userRepository).findByEmail("reporter@example.com");
        verify(userRepository).findById(2L);
        verify(reportRepository).save(any(UserReport.class));
    }

    @Test
    void testCreateUserReport_ReporterNotFound() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userReportService.createUserReport(testRequestDTO, "reporter@example.com");
        });
    }

    @Test
    void testCreateUserReport_ReportedNotFound() {
        // Given
        when(userRepository.findByEmail("reporter@example.com")).thenReturn(Optional.of(testReporter));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userReportService.createUserReport(testRequestDTO, "reporter@example.com");
        });
    }

    @Test
    void testGetAllReports_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of(testReport));
        when(reportRepository.findAllWithUsers(pageable)).thenReturn(page);

        // When
        Map<String, Object> result = userReportService.getAllReports(0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("content"));
        assertTrue(result.containsKey("currentPage"));
        assertTrue(result.containsKey("totalItems"));
        assertTrue(result.containsKey("totalPages"));
        verify(reportRepository).findAllWithUsers(pageable);
    }

    @Test
    void testGetAllReports_InvalidPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of(testReport));
        when(reportRepository.findAllWithUsers(pageable)).thenReturn(page);

        // When
        Map<String, Object> result = userReportService.getAllReports(-1, 10);

        // Then
        assertNotNull(result);
        verify(reportRepository).findAllWithUsers(pageable);
    }

    @Test
    void testGetAllReports_InvalidSize() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of(testReport));
        when(reportRepository.findAllWithUsers(pageable)).thenReturn(page);

        // When
        Map<String, Object> result = userReportService.getAllReports(0, 200);

        // Then
        assertNotNull(result);
        verify(reportRepository).findAllWithUsers(pageable);
    }

    @Test
    void testUpdateReport_Success() {
        // Given
        Map<String, String> body = new HashMap<>();
        body.put("status", "reviewed");
        body.put("actionTaken", "Warning issued");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(reportRepository.save(any(UserReport.class))).thenReturn(testReport);

        // When
        UserReport result = userReportService.updateReport(1L, body, testAdmin);

        // Then
        assertNotNull(result);
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Warning issued", testReport.getActionTaken());
        verify(reportRepository).findById(1L);
        verify(reportRepository).save(testReport);
        verify(activityService).logWithActor(eq(3L), eq("UserReport"), eq(1L), eq("update"), anyString());
    }

    @Test
    void testUpdateReport_NotFound() {
        // Given
        Map<String, String> body = new HashMap<>();
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userReportService.updateReport(1L, body, testAdmin);
        });
    }

    @Test
    void testWarnUser_Success() {
        // Given
        Map<String, String> body = new HashMap<>();
        body.put("message", "You have been warned");
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testReported));
        when(reportRepository.save(any(UserReport.class))).thenReturn(testReport);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        userReportService.warnUser(1L, body);

        // Then
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Warning", testReport.getActionTaken());
        verify(reportRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(notificationRepository).save(any(Notification.class));
        verify(activityService).logWithActor(eq(2L), eq("UserReport"), eq(1L), eq("warn"), anyString());
    }

    @Test
    void testBanUser_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testReported));
        when(userRepository.save(any(User.class))).thenReturn(testReported);
        when(reportRepository.save(any(UserReport.class))).thenReturn(testReport);
        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // When
        userReportService.banUser(1L, testAdmin);

        // Then
        assertEquals("banned", testReported.getStatus());
        assertEquals("reviewed", testReport.getStatus());
        assertEquals("Ban", testReport.getActionTaken());
        verify(userRepository).save(testReported);
        verify(notificationRepository).save(any(Notification.class));
        verify(activityService).logWithActor(eq(3L), eq("UserReport"), eq(1L), eq("ban"), anyString());
    }

    @Test
    void testGetReportById_Success() {
        // Given
        when(reportRepository.findById(1L)).thenReturn(Optional.of(testReport));

        // When
        UserReportDto result = userReportService.getReportById(1L);

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
            userReportService.getReportById(1L);
        });
    }

    @Test
    void testGetReportsByUser_Success() {
        // Given
        when(reportRepository.findAll()).thenReturn(List.of(testReport));

        // When
        List<UserReportDto> result = userReportService.getReportsByUser("reporter@example.com");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reportRepository).findAll();
    }
}

