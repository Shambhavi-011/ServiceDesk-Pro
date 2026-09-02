package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.response.NotificationDto;
import com.servicedeskpro.entity.Notification;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.exception.ForbiddenException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.NotificationRepository;
import com.servicedeskpro.repository.UserRepository;
import com.servicedeskpro.security.UserPrincipal;
import com.servicedeskpro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(boolean unreadOnly) {
        User currentUser = getCurrentAuthenticatedUser();
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(currentUser.getId())
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId());

        return notifications.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        User currentUser = getCurrentAuthenticatedUser();
        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not authorized to modify this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User currentUser = getCurrentAuthenticatedUser();
        notificationRepository.markAllAsReadForUser(currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = getCurrentAuthenticatedUser();
        return notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Unauthenticated user session");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .ticketId(notification.getTicket() != null ? notification.getTicket().getId() : null)
                .ticketNumber(notification.getTicket() != null ? notification.getTicket().getTicketNumber() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}