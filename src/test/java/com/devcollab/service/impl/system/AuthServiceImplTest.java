package com.devcollab.service.impl.system;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private OAuth2User oauth2User;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockExistingUser;

    @BeforeEach
    void setup() {
        // Giả lập 1 entity User trong DB (dùng Mockito để không phụ thuộc constructor
        // thật)
        mockExistingUser = mock(User.class);
        when(mockExistingUser.getUserId()).thenReturn(100L);
        when(mockExistingUser.getName()).thenReturn("Alice");
        when(mockExistingUser.getEmail()).thenReturn("a@gmail.com");
        when(mockExistingUser.getAvatarUrl()).thenReturn(null);
        when(mockExistingUser.getProvider()).thenReturn(null);
    }

    // A01: getUserByEmail - tồn tại
    @Test
    void getUserByEmail_existing_returnsOptionalUser() {
        when(userRepository.findByEmail("a@gmail.com")).thenReturn(Optional.of(mockExistingUser));

        Optional<User> result = authService.getUserByEmail("a@gmail.com");

        assertTrue(result.isPresent());
        assertEquals("a@gmail.com", result.get().getEmail());
        verify(userRepository).findByEmail("a@gmail.com");
    }

    // A02: getUserByEmail - không tồn tại
    @Test
    void getUserByEmail_notFound_returnsEmpty() {
        when(userRepository.findByEmail("x@gmail.com")).thenReturn(Optional.empty());

        Optional<User> result = authService.getUserByEmail("x@gmail.com");

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail("x@gmail.com");
    }

    // A03: getUserByEmail - email null (boundary)
    @Test
    void getUserByEmail_nullEmail_returnsEmptyOptionalByContract() {
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        Optional<User> result = authService.getUserByEmail(null);

        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail(null);
    }

    // A04: getCurrentUser - auth null
    @Test
    void getCurrentUser_authNull_throwsSecurityException() {
        assertThrows(SecurityException.class, () -> authService.getCurrentUser(null));
        verifyNoInteractions(userRepository);
    }

    // A05: OAuth2User - user mới (chưa có trong DB)
    @Test
    void getCurrentUser_oauth2NewUser_returnsTemporaryDTO() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn("new@gmail.com");
        when(oauth2User.getAttribute("name")).thenReturn("New User");
        when(oauth2User.getAttribute("picture")).thenReturn(null);
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());

        UserDTO dto = authService.getCurrentUser(authentication);

        assertNull(dto.getUserId());
        assertEquals("new@gmail.com", dto.getEmail());
        assertEquals("google", dto.getProvider());
        assertNotNull(dto.getAvatarUrl()); // default avatar
        verify(userRepository).findByEmail("new@gmail.com");
    }

    // A06: OAuth2User - thiếu email (boundary)
    @Test
    void getCurrentUser_oauth2MissingEmail_throwsIllegalState() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> authService.getCurrentUser(authentication));
        verifyNoInteractions(userRepository);
    }

    // A07: OAuth2User - user đã tồn tại trong DB
    @Test
    void getCurrentUser_oauth2ExistingUser_returnsDTOFromDB() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("email")).thenReturn("a@gmail.com");
        when(oauth2User.getAttribute("name")).thenReturn("Alice From Google");
        when(oauth2User.getAttribute("picture")).thenReturn("https://img");
        when(userRepository.findByEmail("a@gmail.com")).thenReturn(Optional.of(mockExistingUser));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertEquals("a@gmail.com", dto.getEmail());
        assertEquals("Alice", dto.getName()); // từ DB (UserDTO.fromEntity)
        verify(userRepository).findByEmail("a@gmail.com");
    }

    // A08: Local login - user tồn tại
    @Test
    void getCurrentUser_localExistingUser_returnsDTO() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("a@gmail.com");
        when(userRepository.findByEmail("a@gmail.com")).thenReturn(Optional.of(mockExistingUser));

        UserDTO dto = authService.getCurrentUser(authentication);

        assertEquals("a@gmail.com", dto.getEmail());
        assertEquals("Alice", dto.getName());
        assertEquals("local", dto.getProvider()); // default to local
        assertNotNull(dto.getAvatarUrl());
        verify(userRepository).findByEmail("a@gmail.com");
    }

    // A09: Local login - user không tồn tại
    @Test
    void getCurrentUser_localUserNotFound_throwsIllegalArgument() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("ghost@gmail.com");
        when(userRepository.findByEmail("ghost@gmail.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
        assertEquals("USER_NOT_FOUND", ex.getMessage());
        verify(userRepository).findByEmail("ghost@gmail.com");
    }

    // A10: Unsupported principal type
    @Test
    void getCurrentUser_unsupportedPrincipal_throwsIllegalArgument() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("abc");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.getCurrentUser(authentication));
        assertTrue(ex.getMessage().contains("Unsupported authentication type"));
        verifyNoInteractions(userRepository);
    }
}
