package com.devcollab.service.impl.system;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ✅ Unit Test for AuthServiceImpl
 * Feature: Authentication (OAuth2 + Local + Error handling)
 * Total: 8 Test Cases
 */

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private OAuth2User mockOauth2User;
    private UserDetails mockUserDetails;
    private User mockUser;

    @BeforeEach
    void setup() {
        mockOauth2User = mock(OAuth2User.class);
        mockUserDetails = mock(UserDetails.class);
        mockUser = new User();
        mockUser.setEmail("quan@mail.com");
        mockUser.setName("Quan");
        mockUser.setAvatarUrl("avatar.png");
        mockUser.setProvider("google");
    }

    // ----------------------------
    // TC01: Unauthorized – auth == null
    // ----------------------------
    @Test
    void testGetCurrentUser_AuthIsNull_ShouldThrowSecurityException() {
        SecurityException exception = assertThrows(SecurityException.class,
                () -> authService.getCurrentUser(null));
        assertEquals("Unauthorized", exception.getMessage());
    }

    // ----------------------------
    // TC02: Unauthorized – not authenticated
    // ----------------------------
    @Test
    void testGetCurrentUser_NotAuthenticated_ShouldThrowSecurityException() {
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityException exception = assertThrows(SecurityException.class,
                () -> authService.getCurrentUser(authentication));
        assertEquals("Unauthorized", exception.getMessage());
    }

    // ----------------------------
    // TC03: OAuth2 existing user
    // ----------------------------
    @Test
    void testGetCurrentUser_OAuth2ExistingUser() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockOauth2User);
        when(mockOauth2User.getAttribute("email")).thenReturn("quan@mail.com");
        when(mockOauth2User.getAttribute("name")).thenReturn("Quan");
        when(mockOauth2User.getAttribute("picture")).thenReturn("avatar.png");
        when(userRepository.findByEmail("quan@mail.com")).thenReturn(Optional.of(mockUser));

        UserDTO result = authService.getCurrentUser(authentication);

        assertNotNull(result);
        assertEquals("Quan", result.getName());
        assertEquals("quan@mail.com", result.getEmail());
        assertEquals("google", result.getProvider());
    }

    // ----------------------------
    // TC04: OAuth2 new user (not in DB)
    // ----------------------------
    @Test
    void testGetCurrentUser_OAuth2NewUser() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockOauth2User);
        when(mockOauth2User.getAttribute("email")).thenReturn("new@mail.com");
        when(mockOauth2User.getAttribute("name")).thenReturn("New");
        when(mockOauth2User.getAttribute("picture")).thenReturn(null);
        when(userRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        UserDTO result = authService.getCurrentUser(authentication);

        assertNotNull(result);
        assertEquals("google", result.getProvider());
        assertEquals("new@mail.com", result.getEmail());
        assertTrue(result.getAvatarUrl().contains("https://i.pravatar.cc/100?u="));
    }

    // ----------------------------
    // TC05: OAuth2 missing email
    // ----------------------------
    @Test
    void testGetCurrentUser_OAuth2MissingEmail_ShouldThrowIllegalStateException() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockOauth2User);
        when(mockOauth2User.getAttribute("email")).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authService.getCurrentUser(authentication));
        assertTrue(exception.getMessage().contains("Missing email"));
    }

    // ----------------------------
    // TC06: Local user exists
    // ----------------------------
    @Test
    void testGetCurrentUser_LocalUserExists() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);
        when(mockUserDetails.getUsername()).thenReturn("user@mail.com");

        User existingUser = new User();
        existingUser.setEmail("user@mail.com");
        existingUser.setName("LocalUser");
        existingUser.setProvider("local");
        when(userRepository.findByEmail("user@mail.com")).thenReturn(Optional.of(existingUser));

        UserDTO result = authService.getCurrentUser(authentication);

        assertEquals("user@mail.com", result.getEmail());
        assertEquals("local", result.getProvider());
        assertEquals("LocalUser", result.getName());
    }

    // ----------------------------
    // TC07: Local user not found
    // ----------------------------
    @Test
    void testGetCurrentUser_LocalUserNotFound_ShouldThrowIllegalArgumentException() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);
        when(mockUserDetails.getUsername()).thenReturn("ghost@mail.com");
        when(userRepository.findByEmail("ghost@mail.com")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
        assertEquals("USER_NOT_FOUND", exception.getMessage());
    }

    // ----------------------------
    // TC08: Unsupported principal type
    // ----------------------------
    @Test
    void testGetCurrentUser_UnsupportedPrincipal_ShouldThrowIllegalArgumentException() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(12345); // unsupported object

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
        assertTrue(exception.getMessage().contains("Unsupported authentication type"));
    }
}
