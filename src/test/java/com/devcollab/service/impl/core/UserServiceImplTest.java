package com.devcollab.service.impl.core;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.event.AppEventService;
import com.devcollab.service.system.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppEventService appEventService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPasswordHash("$2a$10$encoded");
        testUser.setProvider("local");
        testUser.setStatus("unverified");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setRoles(new HashSet<>());

        testUserDTO = new UserDTO();
        testUserDTO.setUserId(1L);
        testUserDTO.setEmail("test@example.com");
    }

    @Test
    void testGetAll_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void testGetById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getById(1L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testGetByEmail_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void testCreate_NewUser_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(appEventService).publishUserCreated(any(User.class));

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertEquals("local", testUser.getProvider());
        assertEquals("unverified", testUser.getStatus());
        verify(userRepository).save(testUser);
        verify(appEventService).publishUserCreated(testUser);
    }

    @Test
    void testCreate_ExistingGoogleUser_AddPassword() {
        // Given
        User existingUser = new User();
        existingUser.setUserId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setProvider("google");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        testUser.setPasswordHash("rawPassword");

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertEquals("local_google", existingUser.getProvider());
        verify(userRepository).save(existingUser);
    }

    @Test
    void testCreate_ExistingLocalUser_ThrowsException() {
        // Given
        User existingUser = new User();
        existingUser.setProvider("local");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        testUser.setPasswordHash("rawPassword");

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userService.create(testUser);
        });
    }

    @Test
    void testCreate_ExistingOtpUser_AddPassword() {
        // Given
        User existingUser = new User();
        existingUser.setProvider("otp");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        testUser.setPasswordHash("rawPassword");

        // When
        User result = userService.create(testUser);

        // Then
        assertNotNull(result);
        assertEquals("local", existingUser.getProvider());
        verify(userRepository).save(existingUser);
    }

    @Test
    void testUpdate_Success() {
        // Given
        User patch = new User();
        patch.setName("Updated Name");
        patch.setBio("Updated bio");
        when(authService.getCurrentUser(authentication)).thenReturn(testUserDTO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(appEventService).publishUserStatusChanged(any(User.class));

        // When
        User result = userService.update(1L, patch, authentication);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", testUser.getName());
        assertEquals("Updated bio", testUser.getBio());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdate_DifferentUser_ThrowsException() {
        // Given
        UserDTO otherUser = new UserDTO();
        otherUser.setUserId(2L);
        when(authService.getCurrentUser(authentication)).thenReturn(otherUser);

        // When & Then
        assertThrows(SecurityException.class, () -> {
            userService.update(1L, new User(), authentication);
        });
    }

    @Test
    void testUpdateStatus_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(appEventService).publishUserStatusChanged(any(User.class));

        // When
        User result = userService.updateStatus(1L, "active");

        // Then
        assertNotNull(result);
        assertEquals("active", testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateStatus_InvalidStatus() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            userService.updateStatus(1L, "invalid");
        });
    }

    @Test
    void testUpdateLastSeen_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateLastSeen(1L);

        // Then
        assertNotNull(testUser.getLastSeen());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateLastSeen_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When - Should not throw
        assertDoesNotThrow(() -> {
            userService.updateLastSeen(1L);
        });
    }

    @Test
    void testDelete_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.delete(1L);

        // Then
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userService.delete(1L);
        });
    }

    @Test
    void testInactivate_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.inactivate(1L);

        // Then
        assertEquals("Suspended", testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void testExistsByEmail_Success() {
        // Given
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("test@example.com");

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail("test@example.com");
    }

    @Test
    void testCheckPassword_CorrectPassword() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "$2a$10$encoded")).thenReturn(true);

        // When
        boolean result = userService.checkPassword("test@example.com", "password");

        // Then
        assertTrue(result);
    }

    @Test
    void testCheckPassword_WrongPassword() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        // When
        boolean result = userService.checkPassword("test@example.com", "wrong");

        // Then
        assertFalse(result);
    }

    @Test
    void testMarkVerified_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.markVerified("test@example.com");

        // Then
        assertEquals("verified", testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void testMarkVerified_BannedUser_NoChange() {
        // Given
        testUser.setStatus("banned");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        userService.markVerified("test@example.com");

        // Then
        assertEquals("banned", testUser.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdatePassword_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("$2a$10$newEncoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updatePassword("test@example.com", "newPassword");

        // Then
        assertEquals("$2a$10$newEncoded", testUser.getPasswordHash());
        assertEquals("local", testUser.getProvider());
        verify(userRepository).save(testUser);
    }

    @Test
    void testLoadUserByUsername_Success() {
        // Given
        when(userRepository.findByEmailFetchRoles("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.loadUserByUsername("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
        verify(userRepository).findByEmailFetchRoles("test@example.com");
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        // Given
        when(userRepository.findByEmailFetchRoles("test@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("test@example.com");
        });
    }

    @Test
    void testCountByStatus_Success() {
        // Given
        when(userRepository.countByStatus("active")).thenReturn(10L);

        // When
        long result = userService.countByStatus("active");

        // Then
        assertEquals(10L, result);
        verify(userRepository).countByStatus("active");
    }
}

