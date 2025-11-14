package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.Attachment;
import com.devcollab.domain.Project;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;
import com.devcollab.repository.AttachmentRepository;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.ProjectAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private ProjectAuthorizationService projectAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskFollowerRepository followerRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private Task testTask;
    private Project testProject;
    private User testUser;
    private Attachment testAttachment;

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
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testAttachment = new Attachment();
        testAttachment.setAttachmentId(1L);
        testAttachment.setTask(testTask);
        testAttachment.setFileName("test.pdf");
        testAttachment.setFileUrl("/api/tasks/1/attachments/download/test.pdf");
        testAttachment.setMimeType("application/pdf");
        testAttachment.setFileSize(1024);
        testAttachment.setUploadedBy(testUser);
        testAttachment.setUploadedAt(LocalDateTime.now());
        testAttachment.setVersion(1);
    }

    @Test
    void testGetAttachmentsByTask_Success() {
        // Given
        List<Attachment> attachments = Arrays.asList(testAttachment);
        when(attachmentRepository.findActiveByTaskId(1L)).thenReturn(attachments);

        // When
        List<Attachment> result = attachmentService.getAttachmentsByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(attachmentRepository).findActiveByTaskId(1L);
    }

    @Test
    void testGetAttachmentDTOsByTask_Success() {
        // Given
        List<AttachmentDTO> dtos = Arrays.asList(mock(AttachmentDTO.class));
        when(attachmentRepository.findActiveAttachmentDTOs(1L)).thenReturn(dtos);

        // When
        List<AttachmentDTO> result = attachmentService.getAttachmentDTOsByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(attachmentRepository).findActiveAttachmentDTOs(1L);
    }

    @Test
    void testUploadAttachment_AsPM_Success() throws IOException {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
            when(attachmentRepository.findByTaskAndFileName(testTask, "test.pdf")).thenReturn(List.of());
            when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

            // Mock Files operations
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                mockedFiles.when(() -> Files.copy(any(java.io.InputStream.class), any(Path.class), any())).thenReturn(0L);

                // When
                Attachment result = attachmentService.uploadAttachment(1L, multipartFile, testUser);

                // Then
                assertNotNull(result);
                verify(taskRepository).findById(1L);
                verify(attachmentRepository).save(any(Attachment.class));
                verify(activityService).log(eq("TASK"), eq(1L), eq("ATTACH_FILE"), anyString(), eq(testUser));
            }
        }
    }

    @Test
    void testUploadAttachment_AsFollower_Success() throws IOException {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 1L)).thenReturn(true);
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
            when(attachmentRepository.findByTaskAndFileName(testTask, "test.pdf")).thenReturn(List.of());
            when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                mockedFiles.when(() -> Files.copy(any(java.io.InputStream.class), any(Path.class), any())).thenReturn(0L);

                // When
                Attachment result = attachmentService.uploadAttachment(1L, multipartFile, testUser);

                // Then
                assertNotNull(result);
                verify(followerRepository).existsByTask_TaskIdAndUser_UserId(1L, 1L);
            }
        }
    }

    @Test
    void testUploadAttachment_EmptyFile() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            attachmentService.uploadAttachment(1L, multipartFile, testUser);
        });
    }

    @Test
    void testUploadAttachment_NullUploader() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            attachmentService.uploadAttachment(1L, multipartFile, null);
        });
    }

    @Test
    void testUploadAttachment_TaskNotFound() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            attachmentService.uploadAttachment(1L, multipartFile, testUser);
        });
    }

    @Test
    void testUploadAttachment_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(followerRepository.existsByTask_TaskIdAndUser_UserId(1L, 1L)).thenReturn(false);
            when(multipartFile.isEmpty()).thenReturn(false);

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                attachmentService.uploadAttachment(1L, multipartFile, testUser);
            });
        }
    }

    @Test
    void testUploadAttachment_NewVersion() throws IOException {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
            when(attachmentRepository.findByTaskAndFileName(testTask, "test.pdf")).thenReturn(Arrays.asList(testAttachment));
            when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                mockedFiles.when(() -> Files.copy(any(java.io.InputStream.class), any(Path.class), any())).thenReturn(0L);

                // When
                Attachment result = attachmentService.uploadAttachment(1L, multipartFile, testUser);

                // Then
                assertNotNull(result);
                verify(activityService).log(eq("TASK"), eq(1L), eq("ATTACH_FILE_VERSION"), anyString(), eq(testUser));
            }
        }
    }

    @Test
    void testDeleteAttachment_AsPM_Success() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doNothing().when(authz).ensurePmOfProject("test@example.com", 1L);
            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                // When
                attachmentService.deleteAttachment(1L, "test@example.com");

                // Then
                verify(attachmentRepository).findById(1L);
                verify(attachmentRepository).save(testAttachment);
                verify(activityService).log(eq("TASK"), eq(1L), eq("DELETE_ATTACHMENT"), anyString(), eq(testUser));
            }
        }
    }

    @Test
    void testDeleteAttachment_NotFound() {
        // Given
        when(attachmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            attachmentService.deleteAttachment(1L, "test@example.com");
        });
    }

    @Test
    void testDeleteAttachment_NoPermission() {
        // Given
        try (MockedStatic<SpringContext> mockedSpringContext = mockStatic(SpringContext.class)) {
            ProjectAuthorizationService authz = mock(ProjectAuthorizationService.class);
            mockedSpringContext.when(() -> SpringContext.getBean(ProjectAuthorizationService.class))
                    .thenReturn(authz);
            doThrow(new AccessDeniedException("Not PM")).when(authz).ensurePmOfProject("test@example.com", 1L);
            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            User otherUser = new User();
            otherUser.setUserId(2L);
            otherUser.setEmail("other@example.com");
            testAttachment.setUploadedBy(otherUser);

            // When & Then
            assertThrows(AccessDeniedException.class, () -> {
                attachmentService.deleteAttachment(1L, "test@example.com");
            });
        }
    }
}

