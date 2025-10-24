package com.devcollab.repository;

import com.devcollab.domain.BoardColumn;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
    List<BoardColumn> findByProject_ProjectIdOrderByOrderIndexAsc(Long projectId);
   
    @Query("""
                SELECT c FROM BoardColumn c
                JOIN FETCH c.project p
                WHERE c.id = :id
            """)
    Optional<BoardColumn> findByIdWithProject(@Param("id") Long id);

}
