package com.devcollab.service.impl.feature;

import com.devcollab.domain.Comment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.CommentDTO;
import com.devcollab.repository.CommentRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Task testTask;
    private User testUser;
    private Comment testComment;
    private Comment testParentComment;

    @BeforeEach
    void setUp() {
        testTask = new Task();
        testTask.setTaskId(1L);
        testTask.setTitle("Test Task");

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setAvatarUrl("http://example.com/avatar.jpg");

        testParentComment = new Comment();
        testParentComment.setCommentId(1L);
        testParentComment.setTask(testTask);
        testParentComment.setUser(testUser);
        testParentComment.setContent("Parent comment");
        testParentComment.setCreatedAt(LocalDateTime.now());
        testParentComment.setReplies(new ArrayList<>());

        testComment = new Comment();
        testComment.setCommentId(2L);
        testComment.setTask(testTask);
        testComment.setUser(testUser);
        testComment.setContent("Test comment");
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setParent(null);
        testComment.setReplies(new ArrayList<>());
    }

    @Test
    void testAddComment_Success() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentDTO result = commentService.addComment(1L, 1L, "Test comment", null);

        // Then
        assertNotNull(result);
        verify(taskRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
        verify(activityService).log(eq("TASK"), eq(1L), eq("COMMENT_ADD"), anyString(), eq(testUser));
    }

    @Test
    void testAddComment_TaskNotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.addComment(1L, 1L, "Test comment", null);
        });
    }

    @Test
    void testAddComment_UserNotFound() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.addComment(1L, 1L, "Test comment", null);
        });
    }

    @Test
    void testAddComment_WithMentions() {
        // Given
        String mentionsJson = "[{\"name\":\"user@example.com\",\"email\":\"user@example.com\"}]";
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentDTO result = commentService.addComment(1L, 1L, "Test comment @user@example.com", mentionsJson);

        // Then
        assertNotNull(result);
        verify(notificationService).notifyMentions(eq(testTask), eq(testUser), anyList());
    }

    @Test
    void testReplyToComment_Success() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.of(testParentComment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentDTO result = commentService.replyToComment(1L, 1L, "Reply comment");

        // Then
        assertNotNull(result);
        verify(commentRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
        verify(activityService).log(eq("TASK"), eq(1L), eq("COMMENT_REPLY"), anyString(), eq(testUser));
    }

    @Test
    void testReplyToComment_ParentNotFound() {
        // Given
        when(commentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.replyToComment(1L, 1L, "Reply comment");
        });
    }

    @Test
    void testGetCommentsByTask_Success() {
        // Given
        List<Comment> comments = Arrays.asList(testParentComment);
        when(commentRepository.findByTask_TaskIdAndParentIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(comments);

        // When
        List<CommentDTO> result = commentService.getCommentsByTask(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(commentRepository).findByTask_TaskIdAndParentIsNullOrderByCreatedAtDesc(1L);
    }

    @Test
    void testDeleteComment_Success() {
        // Given
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testComment));
        doNothing().when(commentRepository).delete(any(Comment.class));

        // When
        commentService.deleteComment(2L, 1L);

        // Then
        verify(commentRepository).findById(2L);
        verify(commentRepository).delete(testComment);
        verify(activityService).log(eq("TASK"), eq(1L), eq("COMMENT_DELETE"), anyString(), eq(testUser));
    }

    @Test
    void testDeleteComment_NotFound() {
        // Given
        when(commentRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.deleteComment(2L, 1L);
        });
    }

    @Test
    void testDeleteComment_NotOwner() {
        // Given
        User otherUser = new User();
        otherUser.setUserId(2L);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testComment));

        // When & Then
        assertThrows(SecurityException.class, () -> {
            commentService.deleteComment(2L, 2L);
        });
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void testUpdateComment_Success() {
        // Given
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentDTO result = commentService.updateComment(2L, 1L, "Updated content");

        // Then
        assertNotNull(result);
        assertEquals("Updated content", testComment.getContent());
        verify(commentRepository).save(testComment);
        verify(activityService).log(eq("TASK"), eq(1L), eq("COMMENT_EDIT"), anyString(), eq(testUser));
    }

    @Test
    void testUpdateComment_NotFound() {
        // Given
        when(commentRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            commentService.updateComment(2L, 1L, "Updated content");
        });
    }

    @Test
    void testUpdateComment_NotOwner() {
        // Given
        when(commentRepository.findById(2L)).thenReturn(Optional.of(testComment));

        // When & Then
        assertThrows(SecurityException.class, () -> {
            commentService.updateComment(2L, 2L, "Updated content");
        });
        verify(commentRepository, never()).save(any());
    }
}

