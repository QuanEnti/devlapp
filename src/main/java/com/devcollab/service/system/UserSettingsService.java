package com.devcollab.service.system;

import com.devcollab.domain.User;
import com.devcollab.domain.UserSettings;
import com.devcollab.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {
    private final UserSettingsRepository repo;

    @Transactional
    public UserSettings getOrDefault(User user) {
        return repo.findByUser(user).orElseGet(() -> {
            UserSettings us = new UserSettings();
            us.setUser(user);
            repo.save(us); 
            return us;
        });
    }

    @Transactional
    public void save(UserSettings settings) {
        repo.save(settings);
    }
}
