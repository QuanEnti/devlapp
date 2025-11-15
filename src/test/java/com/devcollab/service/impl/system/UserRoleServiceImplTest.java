package com.devcollab.service.impl.system;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.RoleRepository;
import com.devcollab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserRoleServiceImpl userRoleService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRoles(new HashSet<>());

        userRole = new Role();
        userRole.setRoleId(1L);
        userRole.setName("ROLE_USER");
    }

    @Test
    void testAssignDefaultRole_UserHasNoRole_Success() {
        // Given
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userRoleService.assignDefaultRole(testUser);

        // Then
        assertTrue(testUser.getRoles().contains(userRole));
        verify(roleRepository).findByName("ROLE_USER");
        verify(userRepository).save(testUser);
    }

    @Test
    void testAssignDefaultRole_UserAlreadyHasRole_Skip() {
        // Given
        testUser.getRoles().add(userRole);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        // When
        userRoleService.assignDefaultRole(testUser);

        // Then
        verify(roleRepository).findByName("ROLE_USER");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testAssignDefaultRole_RoleNotFound() {
        // Given
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            userRoleService.assignDefaultRole(testUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }
}

