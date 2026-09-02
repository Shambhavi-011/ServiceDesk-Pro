package com.servicedeskpro.service;

import com.servicedeskpro.dto.response.NotificationDto;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getUserNotifications(boolean unreadOnly);
    void markAsRead(Long notificationId);
    void markAllAsRead();
    long getUnreadCount();
}