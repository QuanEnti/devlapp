package com.devcollab.service.system;

import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface AuthService {
    Optional<User> getUserByEmail(String email);

    UserDTO getCurrentUser(Authentication auth);
}
