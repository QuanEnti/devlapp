package com.devcollab.service.system;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtService jwtService;

    private User mockUser;
    private final String SECRET =
            "0123456789012345678901234567890123456789012345678901234567890123"; // 64-byte key

    @BeforeEach
    void setup() {
        // Inject secret & expiration time
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpMinutes", 1L);

        // Mock user with roles
        Role role1 = new Role();
        role1.setName("ROLE_ADMIN");
        Role role2 = new Role();
        role2.setName("ROLE_MEMBER");

        mockUser = new User();
        mockUser.setEmail("user@mail.com");
        mockUser.getRoles().addAll(Set.of(role1, role2));
    }

    // ✅ TC01: generateAccessToken → token hợp lệ chứa roles
    @Test
    void TC01_generateAccessToken_shouldContainRoles() {
        when(userRepository.findByEmailFetchRoles("user@mail.com"))
                .thenReturn(Optional.of(mockUser));

        String token = jwtService.generateAccessToken("user@mail.com");
        assertAll(() -> assertNotNull(token), () -> assertEquals(3, token.split("\\.").length),
                () -> assertTrue(token.contains(".")));
        verify(userRepository).findByEmailFetchRoles("user@mail.com");
    }

    // ✅ TC02: generateAccessToken → user không tồn tại
    @Test
    void TC02_generateAccessToken_userNotFound_shouldThrow() {
        when(userRepository.findByEmailFetchRoles("ghost@mail.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> jwtService.generateAccessToken("ghost@mail.com"));
    }

    // ✅ TC03: extractEmail() → trả lại subject đúng
    @Test
    void TC03_extractEmail_validToken() {
        when(userRepository.findByEmailFetchRoles("user@mail.com"))
                .thenReturn(Optional.of(mockUser));

        String token = jwtService.generateAccessToken("user@mail.com");
        String email = jwtService.extractEmail(token);
        assertEquals("user@mail.com", email);
    }

    // ✅ TC04: extractRoles() → có roles trong token
    @Test
    void TC04_extractRoles_shouldReturnList() {
        when(userRepository.findByEmailFetchRoles("user@mail.com"))
                .thenReturn(Optional.of(mockUser));

        String token = jwtService.generateAccessToken("user@mail.com");
        List<String> roles = jwtService.extractRoles(token);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_MEMBER"));
    }

    // ✅ TC05: extractRoles() → token không có field roles → fallback
    @Test
    void TC05_extractRoles_noRoles_shouldReturnDefault() {
        String refreshToken = jwtService.generateRefreshToken("nobody@mail.com");
        List<String> roles = jwtService.extractRoles(refreshToken);
        assertEquals(List.of("ROLE_MEMBER"), roles);
    }

    // ✅ TC06: generateRefreshToken → TTL ≈ 7 ngày
    @Test
    void TC06_generateRefreshToken_valid() {
        String token = jwtService.generateRefreshToken("refresh@mail.com");
        assertEquals(3, token.split("\\.").length);
    }

    // ✅ TC07: isValid() → token hợp lệ
    @Test
    void TC07_isValid_true() {
        when(userRepository.findByEmailFetchRoles("user@mail.com"))
                .thenReturn(Optional.of(mockUser));
        String token = jwtService.generateAccessToken("user@mail.com");
        assertTrue(jwtService.isValid(token));
    }

    // ✅ TC08: isValid() → token giả mạo
    @Test
    void TC08_isValid_tampered_shouldReturnFalse() {
        when(userRepository.findByEmailFetchRoles("user@mail.com"))
                .thenReturn(Optional.of(mockUser));
        String token = jwtService.generateAccessToken("user@mail.com");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtService.isValid(tampered));
    }

    // ✅ TC09: isValid() → null token
    @Test
    void TC09_isValid_nullToken_shouldReturnFalse() {
        assertFalse(jwtService.isValid(null));
    }

    // ✅ TC10: extractEmail() → token giả mạo → JwtException
    @Test
    void TC10_extractEmail_malformed_shouldThrow() {
        assertThrows(JwtException.class, () -> jwtService.extractEmail("abc.def.ghi"));
    }
}
