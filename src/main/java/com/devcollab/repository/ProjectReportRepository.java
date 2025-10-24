package com.devcollab.repository;

import com.devcollab.domain.ProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long> {
    @Query("SELECT pr FROM ProjectReport pr JOIN FETCH pr.reporter JOIN FETCH pr.project")
    List<ProjectReport> findAllWithRelations();
    @Query("""
        SELECT pr FROM ProjectReport pr
        LEFT JOIN FETCH pr.reporter
        LEFT JOIN FETCH pr.project
        WHERE pr.id = :id
        """)
    ProjectReport findByIdWithRelations(Long id);

    long countByStatus(String status);

    @Query("SELECT r FROM ProjectReport r " +
            "LEFT JOIN FETCH r.reporter " +
            "LEFT JOIN FETCH r.project")
    List<ProjectReport> findAllWithReporterAndProject();
}
