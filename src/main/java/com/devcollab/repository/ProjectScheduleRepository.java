package com.devcollab.repository;

import com.devcollab.domain.ProjectSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, Long> {

    List<ProjectSchedule> findByProject_ProjectId(Long projectId);

    List<ProjectSchedule> findByProject_ProjectIdAndDatetimeBetween(
            Long projectId, Instant start, Instant end);
}
