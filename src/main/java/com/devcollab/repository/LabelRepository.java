package com.devcollab.repository;

import com.devcollab.domain.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findByProject_ProjectId(Long projectId);
}
