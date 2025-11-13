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
import org.mockito.*;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserReportServiceImplTest {

    @Mock
    private UserReportRepository reportRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private NotificationRepository notificationRepo;
    @Mock
    private ActivityService activityService;

    @InjectMocks
    private UserReportServiceImpl userReportService;

    private User reporter;
    private User reported;
    private UserReport report;
    private ReportRequestDTO dto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        reporter = new User();
        reporter.setUserId(1L);
        reporter.setEmail("reporter@mail.com");

        reported = new User();
        reported.setUserId(2L);
        reported.setEmail("reported@mail.com");

        report = new UserReport();
        report.setId(1L);
        report.setReporter(reporter);
        report.setReported(reported);
        report.setStatus("pending");

        dto = new ReportRequestDTO();
        dto.setReportedId(2L);
        dto.setReason("spam");
        dto.setDetails("bad behavior");
        dto.setProofUrl("http://proof.img");
    }

    // ✅ createUserReport – success
    @Test
    void createUserReport_Success() {
        when(userRepo.findByEmail("reporter@mail.com")).thenReturn(Optional.of(reporter));
        when(userRepo.findById(2L)).thenReturn(Optional.of(reported));

        userReportService.createUserReport(dto, "reporter@mail.com");

        verify(reportRepo, times(1)).save(any(UserReport.class));
    }

    // ❌ reporter not found
    @Test
    void createUserReport_ReporterNotFound() {
        when(userRepo.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                userReportService.createUserReport(dto, "notfound@mail.com"));
    }

    // ❌ reported not found
    @Test
    void createUserReport_ReportedNotFound() {
        when(userRepo.findByEmail("reporter@mail.com")).thenReturn(Optional.of(reporter));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                userReportService.createUserReport(dto, "reporter@mail.com"));
    }

    // ✅ updateReport – success
    @Test
    void updateReport_Success() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "reviewed");
        body.put("actionTaken", "warn");

        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        User admin = new User();
        admin.setUserId(99L);

        UserReport updated = userReportService.updateReport(1L, body, admin);

        assertEquals("reviewed", updated.getStatus());
        assertEquals("warn", updated.getActionTaken());
        verify(reportRepo).save(any(UserReport.class));
        verify(activityService).logWithActor(eq(99L), eq("UserReport"), eq(1L), eq("update"), anyString());
    }

    // ❌ updateReport – not found
    @Test
    void updateReport_NotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.empty());
        User admin = new User();
        admin.setUserId(99L);

        assertThrows(RuntimeException.class,
                () -> userReportService.updateReport(1L, new HashMap<>(), admin));
    }

    // ✅ warnUser – success
    @Test
    void warnUser_Success() {
        Map<String, String> body = Map.of("message", "Please be careful");

        report.setReported(reported);
        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(userRepo.findById(2L)).thenReturn(Optional.of(reported));

        userReportService.warnUser(1L, body);

        verify(reportRepo).save(any(UserReport.class));
        verify(notificationRepo).save(any(Notification.class));
        verify(activityService).logWithActor(eq(2L), eq("UserReport"), eq(1L), eq("warn"), contains("Please be careful"));
    }

    // ❌ warnUser – report not found
    @Test
    void warnUser_ReportNotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userReportService.warnUser(1L, Map.of("message", "warn")));
    }

    // ❌ warnUser – reported not found
    @Test
    void warnUser_ReportedNotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userReportService.warnUser(1L, Map.of("message", "warn")));
    }

    // ✅ banUser – success
    @Test
    void banUser_Success() {
        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(userRepo.findById(2L)).thenReturn(Optional.of(reported));

        User admin = new User();
        admin.setUserId(99L);

        userReportService.banUser(1L, admin);

        assertEquals("banned", reported.getStatus());
        verify(userRepo).save(reported);
        verify(notificationRepo).save(any(Notification.class));
        verify(activityService).logWithActor(eq(99L), eq("UserReport"), eq(1L), eq("ban"), contains("banned"));
    }

    // ❌ banUser – report not found
    @Test
    void banUser_ReportNotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.empty());
        User admin = new User();
        admin.setUserId(99L);

        assertThrows(RuntimeException.class, () -> userReportService.banUser(1L, admin));
    }

    // ❌ banUser – reported not found
    @Test
    void banUser_ReportedNotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        when(userRepo.findById(2L)).thenReturn(Optional.empty());
        User admin = new User();
        admin.setUserId(99L);

        assertThrows(RuntimeException.class, () -> userReportService.banUser(1L, admin));
    }

    // ✅ getReportById – success
    @Test
    void getReportById_Success() {
        when(reportRepo.findById(1L)).thenReturn(Optional.of(report));
        UserReportDto dto = userReportService.getReportById(1L);
        assertNotNull(dto);
    }

    // ❌ getReportById – not found
    @Test
    void getReportById_NotFound() {
        when(reportRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userReportService.getReportById(1L));
    }

    // ✅ getReportsByUser
    @Test
    void getReportsByUser_Success() {
        when(reportRepo.findAll()).thenReturn(List.of(report));
        List<UserReportDto> result = userReportService.getReportsByUser("any@mail.com");
        assertEquals(1, result.size());
    }

    // ✅ getAllReports – success (fixed)
//    @Test
//    void getAllReports_Success() {
//        Pageable pageable = PageRequest.of(0, 10);
//        UserReportDto dto = new UserReportDto(report);
//        Page<UserReportDto> page = new PageImpl<>(List.of(dto), pageable, 1);
//
//        when(reportRepo.findAllWithUsers(any(Pageable.class))).thenReturn(page);
//
//        Map<String, Object> result = userReportService.getAllReports(0, 10);
//
//        assertTrue(result.containsKey("content"));
//        assertTrue(result.containsKey("totalPages"));
//        assertEquals(1, ((List<?>) result.get("content")).size());
//    }
}
