package com.devcollab.repository;

import com.devcollab.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


  @Query("""
      SELECT n
      FROM Notification n
      LEFT JOIN FETCH n.sender
      WHERE n.user.userId = :userId
      ORDER BY n.createdAt DESC
      """)
  List<Notification> findNotificationsByUserId(@Param("userId") Long userId);

  @Query("""
      SELECT n
      FROM Notification n
      LEFT JOIN FETCH n.sender
      WHERE n.user.userId = :userId
        AND LOWER(n.status) = 'unread'
      ORDER BY n.createdAt DESC
      """)
  List<Notification> findUnreadNotificationsByUserId(@Param("userId") Long userId);

  @Query("""
      SELECT COUNT(n)
      FROM Notification n
      WHERE n.user.userId = :userId
        AND LOWER(n.status) = 'unread'
      """)
  int countUnreadByUserId(@Param("userId") Long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Notification n
         SET n.status = 'read',
             n.readAt = CURRENT_TIMESTAMP
       WHERE LOWER(n.status) = 'unread'
         AND n.user.userId = :userId
      """)
  int markAllAsReadByUserId(@Param("userId") Long userId);

  @Query("""
          SELECT n FROM Notification n
          WHERE n.priority = 'MEDIUM'
            AND n.emailed = false
            AND n.status = 'unread'
            AND n.user.email IS NOT NULL
      """)
  List<Notification> findPendingMediumNotifications();

  @Query(value = """
      SELECT TOP 8
          n.notification_id,
          n.user_id,
          u.name AS actorName,
          n.type,
          n.title,
          n.message,
          n.created_at
      FROM Notification n
      LEFT JOIN [User] u ON n.user_id = u.user_id
      WHERE n.reference_id = :projectId
      ORDER BY n.created_at DESC
      """, nativeQuery = true)
  List<Map<String, Object>> findRecentByProject(Long projectId);
}
