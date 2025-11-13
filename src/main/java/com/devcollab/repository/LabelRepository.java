package com.devcollab.repository;

import com.devcollab.domain.Label;
import com.devcollab.dto.LabelDTO;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

        // ✅ Trả trực tiếp LabelDTO – tránh join sâu
        @Query("SELECT new com.devcollab.dto.LabelDTO(l.labelId, l.name, l.color, "
                        + "l.createdBy.userId, l.createdBy.name) "
                        + "FROM Label l WHERE l.labelId = :labelId")
        LabelDTO findDtoById(@Param("labelId") Long labelId);

        // ✅ Giữ nguyên hàm search (projection)
        @Query("SELECT new com.devcollab.dto.LabelDTO(l.labelId, l.name, l.color, "
                        + "l.createdBy.userId, l.createdBy.name) "
                        + "FROM Label l WHERE l.project.projectId = :projectId "
                        + "AND (:keyword IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<LabelDTO> findByProjectAndKeyword(@Param("projectId") Long projectId,
                        @Param("keyword") String keyword);

        boolean existsByProject_ProjectIdAndNameIgnoreCase(Long projectId, String name);

        @Modifying
        @Transactional
        @Query(value = "DELETE FROM [TaskLabel] WHERE task_id = :taskId AND label_id = :labelId",
                        nativeQuery = true)
        void deleteTaskLabel(@Param("taskId") Long taskId, @Param("labelId") Long labelId);

        @Modifying
        @Transactional
        @Query(value = "DELETE FROM [TaskLabel] WHERE label_id = :labelId", nativeQuery = true)
        void deleteAllTaskRelations(@Param("labelId") Long labelId);

}
