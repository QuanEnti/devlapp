    package com.devcollab.repository;

    import com.devcollab.domain.Activity;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.util.List;

    public interface ActivityRepository extends JpaRepository<Activity, Long> {
        List<Activity> findByActor_UserIdOrderByCreatedAtDesc(Long userId);

        List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
        List<Activity> findAllByOrderByCreatedAtDesc();
        Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);

        boolean existsByActor_UserIdAndEntityTypeAndEntityIdAndAction(
                Long actorId, String entityType, Long entityId, String action
        );


    }
