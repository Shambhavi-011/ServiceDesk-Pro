package com.servicedeskpro.service.impl;

import com.servicedeskpro.entity.AuditLog;
import com.servicedeskpro.entity.Notification;
import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.enums.AuditAction;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.repository.AuditLogRepository;
import com.servicedeskpro.repository.NotificationRepository;
import com.servicedeskpro.repository.TicketRepository;
import com.servicedeskpro.service.SlaBreachWatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaBreachWatcherServiceImpl implements SlaBreachWatcherService {

    private final TicketRepository ticketRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    private static final Set<TicketStatus> ACTIVE_STATUSES = Set.of(
            TicketStatus.OPEN,
            TicketStatus.ASSIGNED,
            TicketStatus.IN_PROGRESS,
            TicketStatus.REOPENED
    );

    @Override
    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    @Transactional
    public int checkAndFlagSlaBreaches() {
        Instant now = Instant.now();
        List<Ticket> breachedTickets = ticketRepository.findAll().stream()
                .filter(t -> ACTIVE_STATUSES.contains(t.getStatus()))
                .filter(t -> t.getSlaDueAt() != null && t.getSlaDueAt().isBefore(now))
                .filter(t -> t.getPriority() != TicketPriority.CRITICAL) // Escalate non-critical breached tickets
                .toList();

        if (breachedTickets.isEmpty()) {
            return 0;
        }

        log.warn("SLA Watcher detected {} tickets exceeding SLA target windows. Escalating priority to CRITICAL.", breachedTickets.size());

        for (Ticket ticket : breachedTickets) {
            TicketPriority oldPriority = ticket.getPriority();
            ticket.setPriority(TicketPriority.CRITICAL);
            ticketRepository.save(ticket);

            // 1. Audit Log Escalation
            auditLogRepository.save(AuditLog.builder()
                    .ticket(ticket)
                    .performedBy(ticket.getCreatedBy()) // System event on behalf of ticket context
                    .action(AuditAction.PRIORITY_CHANGED)
                    .oldValue(oldPriority.name())
                    .newValue(TicketPriority.CRITICAL.name())
                    .notes("Automated SLA Breach Watcher: escalated due to exceeded target window")
                    .build());

            // 2. Alert Assigned Agent or Requester
            if (ticket.getAssignedTo() != null) {
                notificationRepository.save(Notification.builder()
                        .recipient(ticket.getAssignedTo())
                        .ticket(ticket)
                        .title("SLA BREACH: " + ticket.getTicketNumber())
                        .message("Ticket has breached target SLA deadline and was escalated to CRITICAL priority.")
                        .read(false)
                        .build());
            }
        }

        return breachedTickets.size();
    }
}