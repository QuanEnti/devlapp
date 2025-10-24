package com.devcollab.service.impl.system;

import com.devcollab.domain.Activity;
import com.devcollab.domain.User;
import com.devcollab.repository.ActivityRepository;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;

    @Override
    public void log(String entityType, Long entityId, String action, String data) {
        try {
            System.out.println("⚠️ [ActivityService] Skip log (no actor) for action: " + action);
        } catch (Exception e) {
            System.err.println("[ActivityService] Error logging: " + e.getMessage());
        }
    }

    public void logWithActor(Long actorId, String entityType, Long entityId, String action, String data) {
        try {
            if (actorId == null) {
                System.out.println("⚠️ [ActivityService] Skip log (actorId=null) for action: " + action);
                return;
            }

            Activity log = new Activity();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction(action);
            log.setDataJson(data);
            log.setCreatedAt(LocalDateTime.now());

            User user = new User();
            user.setUserId(actorId);
            log.setActor(user);

            activityRepository.save(log);

        } catch (Exception e) {
            System.err.println("[ActivityService] Error logging with actor: " + e.getMessage());
        }
    }

    @Override
    public List<Activity> getAllActivities() {
        try {
            return activityRepository.findAllByOrderByCreatedAtDesc();
        } catch (Exception e) {
            System.err.println("[ActivityService] Error fetching logs: " + e.getMessage());
            return List.of();
        }
    }

}
