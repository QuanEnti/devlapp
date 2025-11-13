    package com.devcollab.repository;

    import com.devcollab.domain.Activity;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.List;

    public interface ActivityRepository extends JpaRepository<Activity, Long> {
        List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
        List<Activity> findAllByOrderByCreatedAtDesc();
        Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

        boolean existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
                Long actorId, String entityType, Long entityId, String action
        );
        @Query("""
        SELECT a FROM Activity a
        WHERE
            (:user IS NULL OR LOWER(a.actor.name) LIKE %:user%)
            AND (:action IS NULL OR LOWER(a.action) LIKE %:action%)
            AND (:entityType IS NULL OR LOWER(a.entityType) = :entityType)
        ORDER BY a.createdAt DESC
    """)
        Page<Activity> searchActivities(
                @Param( "user") String user,
                @Param("action") String action,
                @Param("entityType") String entityType,
                Pageable pageable
        );
    }

