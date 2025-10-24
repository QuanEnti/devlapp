package com.devcollab.service.system;

import com.devcollab.domain.Activity;

import java.util.List;

public interface ActivityService {

    /**
     * Ghi log hoạt động hệ thống.
     *
     * @param entityType Loại entity: USER, PROJECT, TASK, COMMENT, ...
     * @param entityId   ID của entity
     * @param action     Hành động: CREATE, UPDATE, DELETE, MOVE, ASSIGN, ...
     * @param data       Thông tin bổ sung (vd: tên task, email user, trạng thái
     *                   mới)
     */
    void log(String entityType, Long entityId, String action, String data);
    
    void logWithActor(Long actorId, String entityType, Long entityId, String action, String data);

    List<Activity> getAllActivities();

}
