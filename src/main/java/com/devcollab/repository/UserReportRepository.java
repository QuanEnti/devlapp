package com.devcollab.repository;

import com.devcollab.domain.UserReport;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    @Query("SELECT r FROM UserReport r " +
            "JOIN FETCH r.reporter " +
            "JOIN FETCH r.reported")
    List<UserReport> findAllWithUsers();
    @Query("SELECT r FROM UserReport r " +
            "JOIN FETCH r.reporter " +
            "JOIN FETCH r.reported " +
            "WHERE r.id = :id")
    UserReport findByIdWithUsers(@Param("id") Long id);

}
