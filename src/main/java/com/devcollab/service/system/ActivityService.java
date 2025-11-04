package com.devcollab.service.system;
import com.devcollab.dto.ActivityDTO;
import com.devcollab.domain.User;

import com.devcollab.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;

public interface ActivityService {

    /**
     * Ghi log hoạt động hệ thống (ví dụ: PROJECT_CREATE, TASK_UPDATE...)
     * 
     * @param entityType Loại đối tượng ("PROJECT", "TASK", "COMMENT", ...)
     * @param entityId   ID đối tượng (projectId, taskId,...)
     * @param action     Hành động ("CREATE", "UPDATE", "DELETE", "ADD_MEMBER", ...)
     * @param message    Nội dung chi tiết (tùy chọn)
     * @param actor      Người thực hiện (User)
     */
    void log(String entityType, Long entityId, String action, String message, User actor);

    /**
     * Ghi log nhưng không cần truyền actor (dùng cho hệ thống tự động)
     */
    void log(String entityType, Long entityId, String action, String message);

    /**
     * Lấy danh sách activity của 1 Task/Project theo ID
     */
    List<ActivityDTO> getActivities(String entityType, Long entityId);
    List<Activity> getAllActivities();
    public Page<Activity> getPaginatedActivities(Pageable pageable);
    void logWithActor(Long actorId, String entityType, Long entityId, String action, String data);

}
