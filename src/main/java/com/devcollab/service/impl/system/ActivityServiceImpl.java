package com.devcollab.service.impl.system;

import com.devcollab.domain.Activity;
import com.devcollab.domain.User;
import com.devcollab.dto.ActivityDTO;
import com.devcollab.repository.ActivityRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.system.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepo;
    private final UserRepository userRepo;

    @Override
    public void log(String entityType, Long entityId, String action, String message, User actor) {
        try {
            if (entityType == null || entityId == null || action == null) {
                log.warn("Bỏ qua log vì thiếu dữ liệu bắt buộc: entityType={}, entityId={}, action={}",
                        entityType, entityId, action);
                return;
            }

            Activity activity = new Activity();
            activity.setEntityType(entityType);
            activity.setEntityId(entityId);
            activity.setAction(action);
            activity.setDataJson(message);
            activity.setCreatedAt(LocalDateTime.now());

            // 🟢 Kiểm tra actor hợp lệ
            if (actor != null) {
                if (actor.getUserId() != null) {
                    // Reload lại từ DB để tránh Detached entity
                    userRepo.findById(actor.getUserId()).ifPresent(activity::setActor);
                } else {
                    log.warn("⚠️ Actor chưa có userId, log sẽ không có thông tin người thực hiện");
                }
            }

            activityRepo.save(activity);
            log.info("🪶 Logged activity: [{}#{}] {} by {}",
                    entityType, entityId, action,
                    actor != null ? actor.getName() : "System");

        } catch (Exception e) {
            log.error("❌ Lỗi khi ghi activity log: {}", e.getMessage());
        }
    }

    /**
     * ✅ Ghi log hệ thống tự động (không có actor)
     */
    @Override
    public void log(String entityType, Long entityId, String action, String message) {
        log(entityType, entityId, action, message, null);
    }

    /**
     * ✅ Lấy danh sách hoạt động theo entity (Project / Task)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivities(String entityType, Long entityId) {
        return activityRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream()
                .map(a -> new ActivityDTO(
                        a.getActivityId(),
                        a.getEntityType(),
                        a.getEntityId(),
                        a.getAction(),
                        a.getActor() != null ? a.getActor().getUserId() : null,
                        a.getActor() != null ? a.getActor().getName() : "System",
                        a.getActor() != null ? a.getActor().getAvatarUrl() : null,
                        a.getDataJson(),
                        a.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
