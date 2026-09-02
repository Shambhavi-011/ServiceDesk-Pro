package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.response.DashboardStatsDto;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.exception.ForbiddenException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.NotificationRepository;
import com.servicedeskpro.repository.TicketRepository;
import com.servicedeskpro.repository.UserRepository;
import com.servicedeskpro.security.UserPrincipal;
import com.servicedeskpro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        User currentUser = getCurrentAuthenticatedUser();
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isAgent = currentUser.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_SUPPORT_AGENT);

        long unreadNotifs = notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId());
        Map<String, Long> metrics = new HashMap<>();
        String roleLabel;

        if (isAdmin) {
            roleLabel = "ADMIN";
            metrics.put("totalTickets", ticketRepository.count());
            metrics.put("openTickets", ticketRepository.countByStatus(TicketStatus.OPEN));
            metrics.put("assignedTickets", ticketRepository.countByStatus(TicketStatus.ASSIGNED));
            metrics.put("inProgressTickets", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS));
            metrics.put("resolvedTickets", ticketRepository.countByStatus(TicketStatus.RESOLVED));
            metrics.put("closedTickets", ticketRepository.countByStatus(TicketStatus.CLOSED));
            metrics.put("criticalTickets", ticketRepository.countByPriority(TicketPriority.CRITICAL));
            metrics.put("totalUsers", userRepository.count());
        } else if (isAgent) {
            roleLabel = "SUPPORT_AGENT";
            metrics.put("assignedToMe", ticketRepository.countByAssignedToIdAndStatus(currentUser.getId(), TicketStatus.ASSIGNED));
            metrics.put("inProgressByMe", ticketRepository.countByAssignedToIdAndStatus(currentUser.getId(), TicketStatus.IN_PROGRESS));
            metrics.put("resolvedByMe", ticketRepository.countByAssignedToIdAndStatus(currentUser.getId(), TicketStatus.RESOLVED));
            metrics.put("unassignedQueue", ticketRepository.countByStatus(TicketStatus.OPEN));
            metrics.put("criticalTickets", ticketRepository.countByPriority(TicketPriority.CRITICAL));
        } else {
            roleLabel = "EMPLOYEE";
            metrics.put("myOpenTickets", ticketRepository.countByCreatedByIdAndStatus(currentUser.getId(), TicketStatus.OPEN));
            metrics.put("myAssignedTickets", ticketRepository.countByCreatedByIdAndStatus(currentUser.getId(), TicketStatus.ASSIGNED));
            metrics.put("myInProgressTickets", ticketRepository.countByCreatedByIdAndStatus(currentUser.getId(), TicketStatus.IN_PROGRESS));
            metrics.put("myResolvedTickets", ticketRepository.countByCreatedByIdAndStatus(currentUser.getId(), TicketStatus.RESOLVED));
            metrics.put("myClosedTickets", ticketRepository.countByCreatedByIdAndStatus(currentUser.getId(), TicketStatus.CLOSED));
        }

        return DashboardStatsDto.builder()
                .userRole(roleLabel)
                .metrics(metrics)
                .unreadNotificationsCount(unreadNotifs)
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Unauthenticated user session");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }
}