package com.devcollab.service.impl.system;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.RoleRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    @Override
    public void assignDefaultRole(User user){
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ROLE_USER trong hệ thống"));

        boolean hasUserRole = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_USER"));

        if (!hasUserRole) {
            user.getRoles().add(userRole);
            userRepository.save(user);
        }
    }
}
