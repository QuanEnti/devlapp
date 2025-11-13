package com.devcollab.repository;

import com.devcollab.domain.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    List<JoinRequest> findByProject_ProjectIdAndStatus(Long projectId, String status);

    Optional<JoinRequest> findByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

    boolean existsByProject_ProjectIdAndUser_UserIdAndStatus(Long projectId, Long userId,
            String status);
}
