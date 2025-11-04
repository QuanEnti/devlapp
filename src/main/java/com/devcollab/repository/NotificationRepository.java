package com.devcollab.repository;

import com.devcollab.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UserId(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.email = :email AND n.status = 'unread'")
    int countUnreadByUserEmail(@Param("email") String email);

    @Query("SELECT n FROM Notification n WHERE n.user.email = :email ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsByUserEmail(@Param("email") String email);

//    boolean existsByUser_UserIdAndTypeAndReferenceId(Long userId, String type, Long referenceId);

}
