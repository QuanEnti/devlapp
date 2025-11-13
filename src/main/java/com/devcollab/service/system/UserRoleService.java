package com.devcollab.service.system;

import com.devcollab.domain.User;
import org.springframework.stereotype.Service;

public interface UserRoleService {
    void assignDefaultRole(User user);
}
