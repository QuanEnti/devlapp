package com.devcollab.repository;

import com.devcollab.domain.Attachment;
import com.devcollab.domain.Task;
import com.devcollab.dto.AttachmentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskAndFileName(Task task, String fileName);

    @Query("""
            SELECT new com.devcollab.dto.AttachmentDTO(
                a.attachmentId,
                a.fileName,
                a.fileUrl,
                a.mimeType,
                a.fileSize,
                a.uploadedAt,
                new com.devcollab.dto.AttachmentMemberInfo(
                    u.userId, u.name, u.avatarUrl
                )
            )
            FROM Attachment a
            JOIN a.uploadedBy u
            WHERE a.task.taskId = :taskId AND a.deletedAt IS NULL
            ORDER BY a.uploadedAt DESC
            """)
    List<AttachmentDTO> findActiveAttachmentDTOs(@Param("taskId") Long taskId);

    @Query("SELECT a FROM Attachment a WHERE a.task.taskId = :taskId AND a.deletedAt IS NULL ORDER BY a.uploadedAt DESC")
    List<Attachment> findActiveByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE Attachment a SET a.deletedAt = CURRENT_TIMESTAMP WHERE a.attachmentId = :id")
    void softDelete(@Param("id") Long id);

    Optional<Attachment> findByTaskAndFileUrl(Task task, String fileUrl);

    @Query("""
                SELECT new com.devcollab.dto.AttachmentDTO(
                    a.attachmentId,
                    a.fileName,
                    a.fileUrl,
                    a.mimeType,
                    COALESCE(a.fileSize, 0),
                    a.uploadedAt,
                    new com.devcollab.dto.AttachmentMemberInfo(
                        u.userId,
                        u.name,
                        u.avatarUrl
                    )
                )
                FROM Attachment a
                LEFT JOIN a.uploadedBy u
                WHERE (u.userId = :userId OR a.uploadedBy IS NULL)
                  AND a.deletedAt IS NULL
                ORDER BY a.uploadedAt DESC
            """)
    List<AttachmentDTO> findRecentLinksByUser(@Param("userId") Long userId);

}
