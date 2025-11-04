package com.devcollab.service.system;

import com.devcollab.domain.Notification;
import com.devcollab.domain.User;

public interface WebSocketNotificationService {


    void sendToUser(User receiver, Notification notification, User sender);
}
