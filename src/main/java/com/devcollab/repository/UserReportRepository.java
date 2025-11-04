package com.devcollab.repository;

import com.devcollab.domain.UserReport;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    @Query(value = "SELECT r FROM UserReport r " +
            "LEFT JOIN FETCH r.reporter rep " +
            "LEFT JOIN FETCH r.reported usr",
            countQuery = "SELECT COUNT(r) FROM UserReport r")
    Page<UserReport> findAllWithUsers(Pageable pageable);
    @Query("SELECT r FROM UserReport r " +
            "JOIN FETCH r.reporter " +
            "JOIN FETCH r.reported " +
            "WHERE r.id = :id")
    UserReport findByIdWithUsers(@Param("id") Long id);

    Optional<UserReport> findById(Long id);



}
