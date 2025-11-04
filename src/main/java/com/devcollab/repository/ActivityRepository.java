    package com.devcollab.repository;

    import com.devcollab.domain.Activity;
    import org.springframework.data.jpa.repository.JpaRepository;
    import java.util.List;

    public interface ActivityRepository extends JpaRepository<Activity, Long> {
        List<Activity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    }