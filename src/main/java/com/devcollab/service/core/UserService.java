    package com.devcollab.service.core;

    import com.devcollab.domain.User;
    import jakarta.transaction.Transactional;
    import org.springframework.security.core.userdetails.UserDetails;
    import java.util.List;
    import java.util.Optional;

    public interface UserService {

        List<User> getAll();

        Optional<User> getById(Long id);

        Optional<User> getByEmail(String email);

        User create(User user);

        User update(Long id, User patch);

        User updateStatus(Long id, String status);

        void updateLastSeen(Long userId);

        void delete(Long id);

        @Transactional
        void inactivate(Long id);

        boolean existsByEmail(String email);

        boolean checkPassword(String email, String rawPassword);

        void markVerified(String email);

        void updatePassword(String email, String newPassword);


        UserDetails loadUserByUsername(String username);
    }
