package com.devcollab.service.impl.system;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser;
    private OAuth2User oauthUser;
    private UserDetails localUser;

    @BeforeEach
    void setup() {
        existingUser = new User();
        existingUser.setUserId(1L);
        existingUser.setEmail("user@example.com");
        existingUser.setName("Test User");
        existingUser.setProvider("local");
        existingUser.setAvatarUrl("avatar.png");
        existingUser.setStatus("active");

        Role memberRole = new Role();
        memberRole.setRoleId(1L);
        memberRole.setName("ROLE_MEMBER");

        existingUser.getRoles().add(memberRole);

        oauthUser = mock(OAuth2User.class);
        localUser = mock(UserDetails.class);
    }

    // 🧩 TC01: auth == null → Unauthorized
    @Test
    void TC01_authNull_shouldThrowSecurityException() {
        assertThrows(SecurityException.class, () -> authService.getCurrentUser(null));
    }

    // 🧩 TC02: not authenticated → Unauthorized
    @Test
    void TC02_notAuthenticated_shouldThrowSecurityException() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(SecurityException.class, () -> authService.getCurrentUser(authentication));
    }

    // 🧩 TC03: OAuth2 user tồn tại trong DB
    @Test
    void TC03_oauthExistingUser_shouldReturnDTO() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("email")).thenReturn("user@example.com");
        when(oauthUser.getAttribute("name")).thenReturn("Quan");
        when(oauthUser.getAttribute("picture")).thenReturn("pic.png");
        when(userRepository.findByEmailFetchRoles("user@example.com"))
                .thenReturn(Optional.of(existingUser));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertAll(() -> assertEquals("user@example.com", dto.getEmail()),
                () -> assertEquals("Test User", dto.getName()));
    }

    // 🧩 TC04: OAuth2 user mới (chưa tồn tại)
    @Test
    void TC04_oauthNewUser_shouldBeCreated() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("email")).thenReturn("new@mail.com");
        when(oauthUser.getAttribute("name")).thenReturn("New");
        when(oauthUser.getAttribute("picture")).thenReturn("avatar.png");
        when(userRepository.findByEmailFetchRoles("new@mail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertEquals("new@mail.com", dto.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // 🧩 TC05: OAuth2 thiếu email → IllegalStateException
    @Test
    void TC05_oauthMissingEmail_shouldThrow() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("email")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> authService.getCurrentUser(authentication));
    }

    // 🧩 TC06: Local user tồn tại → return DTO
    @Test
    void TC06_localUserFound_shouldReturnDTO() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(localUser);
        when(localUser.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmailFetchRoles("user@example.com"))
                .thenReturn(Optional.of(existingUser));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertEquals("user@example.com", dto.getEmail());
        assertEquals("Test User", dto.getName());
    }

    // 🧩 TC07: Local user không tồn tại → IllegalArgumentException
    @Test
    void TC07_localUserNotFound_shouldThrow() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(localUser);
        when(localUser.getUsername()).thenReturn("ghost@mail.com");
        when(userRepository.findByEmailFetchRoles("ghost@mail.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
    }

    // 🧩 TC08: Principal không hợp lệ → IllegalArgumentException
    @Test
    void TC08_unsupportedPrincipal_shouldThrow() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(123);
        assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
    }

    // 🧩 TC09: Principal null → IllegalArgumentException
    @Test
    void TC09_principalNull_shouldThrowUnsupported() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(null);
        assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
    }

    // 🧩 TC10: OAuth2 name null → fallback sang phần trước @
    @Test
    void TC10_oauthNameNull_shouldFallback() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("email")).thenReturn("abc@gmail.com");
        when(oauthUser.getAttribute("name")).thenReturn(null);
        when(oauthUser.getAttribute("picture")).thenReturn("pic.png");
        when(userRepository.findByEmailFetchRoles("abc@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertTrue(dto.getName().startsWith("abc"));
    }
}
