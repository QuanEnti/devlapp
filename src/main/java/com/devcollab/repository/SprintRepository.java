package com.devcollab.repository;

import com.devcollab.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProject_ProjectId(Long projectId);
}
