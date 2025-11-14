package com.devcollab.service.impl.feature;

import com.devcollab.domain.Message;
import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.MessageRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User testUser;
    private Project testProject;
    private Message testMessage;
    private MessageRequestDTO testRequestDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");

        testProject = new Project();
        testProject.setProjectId(1L);
        testProject.setName("Test Project");

        testMessage = new Message();
        testMessage.setMessageId(1L);
        testMessage.setSender(testUser);
        testMessage.setProject(testProject);
        testMessage.setContent("Test message content");
        testMessage.setCreatedAt(LocalDateTime.now());

        testRequestDTO = new MessageRequestDTO();
        testRequestDTO.setProjectId(1L);
        testRequestDTO.setContent("New message");
    }

    @Test
    void testGetMessagesByProjectId_Success() {
        // Given
        List<Message> messages = Arrays.asList(testMessage);
        when(messageRepository.findByProject_ProjectIdOrderByCreatedAtAsc(1L)).thenReturn(messages);

        // When
        List<MessageResponseDTO> result = messageService.getMessagesByProjectId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getSenderName());
        assertEquals("test@example.com", result.get(0).getSenderEmail());
        assertEquals("Test message content", result.get(0).getContent());
        verify(messageRepository).findByProject_ProjectIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void testGetMessagesByProjectId_EmptyList() {
        // Given
        when(messageRepository.findByProject_ProjectIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        // When
        List<MessageResponseDTO> result = messageService.getMessagesByProjectId(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(messageRepository).findByProject_ProjectIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void testSendMessage_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        Message result = messageService.sendMessage("test@example.com", testRequestDTO);

        // Then
        assertNotNull(result);
        verify(userRepository).findByEmail("test@example.com");
        verify(projectRepository).findById(1L);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void testSendMessage_UserNotFound() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            messageService.sendMessage("test@example.com", testRequestDTO);
        });
        verify(projectRepository, never()).findById(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void testSendMessage_ProjectNotFound() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            messageService.sendMessage("test@example.com", testRequestDTO);
        });
        verify(messageRepository, never()).save(any());
    }
}

