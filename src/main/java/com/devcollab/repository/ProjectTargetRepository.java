package com.devcollab.repository;

import com.devcollab.domain.ProjectTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProjectTargetRepository extends JpaRepository<ProjectTarget, Long> {

    @Query("""
                SELECT pt
                FROM ProjectTarget pt
                WHERE pt.month = :month
                  AND pt.year = :year
                  AND pt.createdBy = :pmId
            """)
    Optional<ProjectTarget> findByMonthYearAndPm(
            @Param("month") int month,
            @Param("year") int year,
            @Param("pmId") Long pmId);

    @Query("""
                SELECT pt
                FROM ProjectTarget pt
                WHERE pt.year = :year
                  AND pt.createdBy = :pmId
                ORDER BY pt.month
            """)
    List<ProjectTarget> findTargetsByYearAndPm(
            @Param("year") int year,
            @Param("pmId") Long pmId);
}
