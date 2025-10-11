package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDto;
import com.devcollab.service.core.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserRestController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // --- REST endpoints ---

    @GetMapping
    public List<UserDto> getAllUsers() {
        System.out.println("DEBUG: getAllUsers called");
        return userService.getAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        System.out.println("DEBUG: getUser called with id=" + id);
        return userService.getById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User not found id=" + id));
    }

    @PostMapping
    public UserDto createUser(@RequestBody UserDto dto) {
        System.out.println("DEBUG: createUser called with dto=" + dto);
        try {
            User user = toEntity(dto);
            return toDto(userService.create(user)); // service will encode password + set timestamps
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable Long id, @RequestBody UserDto dto) {
        System.out.println("DEBUG: updateUser called with id=" + id + " dto=" + dto);
        try {
            User existing = userService.getById(id)
                    .orElseThrow(() -> new RuntimeException("User not found id=" + id));

            // Update fields
            existing.setEmail(dto.getEmail());
            existing.setName(dto.getName());
            existing.setStatus(dto.getStatus());
            existing.setSkills(dto.getSkills());
            existing.setAvatarUrl(dto.getAvatarUrl());
            existing.setBio(dto.getBio());
            existing.setTimezone(dto.getTimezone());
            existing.setPreferredLanguage(dto.getPreferredLanguage());
            existing.setUpdatedAt(LocalDateTime.now());

            // Update password if provided
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                existing.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            }

            return toDto(userService.update(id, existing));
        } catch (Exception e) {
            e.printStackTrace(); // print full error to console
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        System.out.println("DEBUG: inactive User called with id=" + id);
        try {
            userService.inactivate(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    // --- Mapping helpers ---

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setStatus(user.getStatus());
        dto.setSkills(user.getSkills());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setTimezone(user.getTimezone());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    private User toEntity(UserDto dto) {
        User user = new User();
        user.setUserId(dto.getUserId());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setStatus(dto.getStatus());
        user.setSkills(dto.getSkills());
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setBio(dto.getBio());
        user.setPreferredLanguage(dto.getPreferredLanguage());
        user.setTimezone(dto.getTimezone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPasswordHash(dto.getPassword());
        }

        return user;
    }
}
