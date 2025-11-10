package com.devcollab.repository;

import com.devcollab.domain.PendingInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingInviteRepository extends JpaRepository<PendingInvite, Long> {
    Optional<PendingInvite> findByEmailAndAcceptedFalse(String email);

    boolean existsByEmailAndAcceptedFalse(String email);
}
