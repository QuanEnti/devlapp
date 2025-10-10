package com.devcollab.repository;

import com.devcollab.domain.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
    List<BoardColumn> findByProject_ProjectIdOrderByOrderIndexAsc(Long projectId);
}
