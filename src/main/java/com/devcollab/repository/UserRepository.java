package com.devcollab.repository;

import com.devcollab.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    long countByStatus(String status);

    @Query(value = """
    SELECT FORMAT(created_at, 'yyyy-MM') as month, COUNT(*) as count
    FROM User
    GROUP BY FORMAT(created_at, 'yyyy-MM')
    ORDER BY month
    """, nativeQuery = true)
    List<Object[]> countUsersByMonth();
}

