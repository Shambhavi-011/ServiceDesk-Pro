package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.request.*;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.TicketDetailDto;
import com.servicedeskpro.dto.response.TicketSummaryDto;
import com.servicedeskpro.dto.response.UserResponseDto;
import com.servicedeskpro.entity.*;
import com.servicedeskpro.entity.enums.AuditAction;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.exception.ForbiddenException;
import com.servicedeskpro.exception.InvalidStateTransitionException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.*;
import com.servicedeskpro.security.UserPrincipal;
import com.servicedeskpro.service.TicketService;
import com.servicedeskpro.specification.TicketSpecification;
import com.servicedeskpro.util.TicketNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public TicketDetailDto createTicket(CreateTicketRequestDto request) {
        User currentUser = getCurrentAuthenticatedUser();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Instant now = Instant.now();
        Instant slaDueAt = now.plus(category.getDefaultSlaHours(), ChronoUnit.HOURS);

        Ticket ticket = Ticket.builder()
                .ticketNumber(TicketNumberGenerator.generate())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .priority(request.getPriority())
                .category(category)
                .createdBy(currentUser)
                .slaDueAt(slaDueAt)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        recordAuditLog(savedTicket, currentUser, AuditAction.TICKET_CREATED, null, TicketStatus.OPEN.name(), "Ticket logged by employee");

        log.info("Ticket created: {} by user: {}", savedTicket.getTicketNumber(), currentUser.getUsername());
        return mapToDetailDto(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailDto getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        validateTicketReadAccess(ticket, currentUser);

        return mapToDetailDto(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryDto> getTickets(
            int page,
            int size,
            String sortBy,
            String sortDir,
            TicketStatus status,
            TicketPriority priority,
            Long categoryId,
            Long assignedToId,
            Long createdById,
            String search,
            Boolean myTickets,
            Boolean unassigned) {

        User currentUser = getCurrentAuthenticatedUser();

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Ticket> spec = TicketSpecification.buildFilter(
                currentUser, status, priority, categoryId, assignedToId, createdById, search, myTickets, unassigned
        );

        Page<Ticket> ticketPage = ticketRepository.findAll(spec, pageable);
        Page<TicketSummaryDto> dtoPage = ticketPage.map(this::mapToSummaryDto);

        return PageResponse.of(dtoPage);
    }

    @Override
    @Transactional
    public TicketDetailDto updateTicket(Long id, UpdateTicketRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not authorized to modify this ticket");
        }

        if (!isAdmin && ticket.getStatus() != TicketStatus.OPEN) {
            throw new InvalidStateTransitionException("Ticket details can only be modified while in OPEN status");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            ticket.setCategory(category);
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }

        return mapToDetailDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketDetailDto updateStatus(Long id, UpdateStatusRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAgent = isUserInRole(currentUser, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAgent && !isAdmin) {
            throw new ForbiddenException("Only Support Agents and Admins can update ticket status");
        }

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus targetStatus = request.getStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot transition ticket status from %s to %s", currentStatus, targetStatus));
        }

        ticket.setStatus(targetStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        recordAuditLog(updatedTicket, currentUser, AuditAction.STATUS_CHANGED,
                currentStatus.name(), targetStatus.name(), request.getNotes());

        createNotification(ticket.getCreatedBy(), ticket,
                "Ticket Status Updated",
                String.format("Your ticket %s status is now %s", ticket.getTicketNumber(), targetStatus));

        return mapToDetailDto(updatedTicket);
    }

    @Override
    @Transactional
    public TicketDetailDto assignTicket(Long id, AssignTicketRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAgent = isUserInRole(currentUser, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAgent && !isAdmin) {
            throw new ForbiddenException("Only Support Agents and Admins can assign tickets");
        }

        User targetAgent;
        if (request.getAgentId() != null) {
            if (!isAdmin) {
                throw new ForbiddenException("Only Admins can reassign tickets to other agents");
            }
            targetAgent = userRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAgentId()));
        } else {
            targetAgent = currentUser;
        }

        String previousAssignee = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : "UNASSIGNED";
        ticket.setAssignedTo(targetAgent);

        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        recordAuditLog(updatedTicket, currentUser, AuditAction.AGENT_ASSIGNED,
                previousAssignee, targetAgent.getUsername(), request.getNotes());

        createNotification(ticket.getCreatedBy(), ticket,
                "Ticket Assigned",
                String.format("Ticket %s has been assigned to agent %s %s",
                        ticket.getTicketNumber(), targetAgent.getFirstName(), targetAgent.getLastName()));

        if (!targetAgent.getId().equals(currentUser.getId())) {
            createNotification(targetAgent, ticket,
                    "New Ticket Assigned to You",
                    String.format("Ticket %s has been assigned to your queue", ticket.getTicketNumber()));
        }

        return mapToDetailDto(updatedTicket);
    }

    @Override
    @Transactional
    public TicketDetailDto resolveTicket(Long id, ResolveTicketRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAgent = isUserInRole(currentUser, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAgent && !isAdmin) {
            throw new ForbiddenException("Only Support Agents and Admins can resolve tickets");
        }

        if (!ticket.getStatus().canTransitionTo(TicketStatus.RESOLVED)) {
            throw new InvalidStateTransitionException(
                    String.format("Cannot resolve ticket in %s status. Must be IN_PROGRESS or REOPENED", ticket.getStatus()));
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionNotes(request.getResolutionNotes());
        ticket.setResolvedAt(Instant.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        recordAuditLog(updatedTicket, currentUser, AuditAction.TICKET_RESOLVED,
                oldStatus.name(), TicketStatus.RESOLVED.name(), "Resolution added: " + request.getResolutionNotes());

        createNotification(ticket.getCreatedBy(), ticket,
                "Ticket Resolved — Action Required",
                String.format("Ticket %s has been marked as RESOLVED. Please verify and confirm resolution.", ticket.getTicketNumber()));

        return mapToDetailDto(updatedTicket);
    }

    @Override
    @Transactional
    public TicketDetailDto reopenTicket(Long id, ReopenTicketRequestDto request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the ticket creator or an Admin can reopen a ticket");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidStateTransitionException("Only RESOLVED tickets can be reopened");
        }

        ticket.setStatus(TicketStatus.REOPENED);
        Ticket updatedTicket = ticketRepository.save(ticket);

        recordAuditLog(updatedTicket, currentUser, AuditAction.TICKET_REOPENED,
                TicketStatus.RESOLVED.name(), TicketStatus.REOPENED.name(), "Reopen Reason: " + request.getReason());

        if (ticket.getAssignedTo() != null) {
            createNotification(ticket.getAssignedTo(), ticket,
                    "Ticket Reopened",
                    String.format("Ticket %s was reopened by %s: %s",
                            ticket.getTicketNumber(), currentUser.getUsername(), request.getReason()));
        }

        return mapToDetailDto(updatedTicket);
    }

    @Override
    @Transactional
    public TicketDetailDto closeTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);

        if (!isAdmin && !ticket.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Only the ticket creator or an Admin can confirm resolution and close the ticket");
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED && !isAdmin) {
            throw new InvalidStateTransitionException("Ticket must be in RESOLVED status to be closed");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(Instant.now());

        Ticket updatedTicket = ticketRepository.save(ticket);

        recordAuditLog(updatedTicket, currentUser, AuditAction.TICKET_CLOSED,
                oldStatus.name(), TicketStatus.CLOSED.name(), "Resolution verified and closed by requester");

        return mapToDetailDto(updatedTicket);
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenException("Unauthenticated user session");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
    }

    private boolean isUserInRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == roleName);
    }

    private void validateTicketReadAccess(Ticket ticket, User user) {
        boolean isAgent = isUserInRole(user, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(user, RoleName.ROLE_ADMIN);

        if (!isAgent && !isAdmin && !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to view this ticket");
        }
    }

    private void recordAuditLog(Ticket ticket, User user, AuditAction action, String oldValue, String newValue, String notes) {
        AuditLog auditLog = AuditLog.builder()
                .ticket(ticket)
                .performedBy(user)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .notes(notes)
                .build();
        auditLogRepository.save(auditLog);
    }

    private void createNotification(User recipient, Ticket ticket, String title, String message) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .ticket(ticket)
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    private TicketSummaryDto mapToSummaryDto(Ticket ticket) {
        return TicketSummaryDto.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .categoryName(ticket.getCategory().getName())
                .createdByFullName(ticket.getCreatedBy().getFirstName() + " " + ticket.getCreatedBy().getLastName())
                .createdByUsername(ticket.getCreatedBy().getUsername())
                .assignedToFullName(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName() : null)
                .assignedToUsername(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getUsername() : null)
                .slaDueAt(ticket.getSlaDueAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private TicketDetailDto mapToDetailDto(Ticket ticket) {
        return TicketDetailDto.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .categoryId(ticket.getCategory().getId())
                .categoryName(ticket.getCategory().getName())
                .createdBy(mapToUserResponseDto(ticket.getCreatedBy()))
                .assignedTo(ticket.getAssignedTo() != null ? mapToUserResponseDto(ticket.getAssignedTo()) : null)
                .resolutionNotes(ticket.getResolutionNotes())
                .slaDueAt(ticket.getSlaDueAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private UserResponseDto mapToUserResponseDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .department(user.getDepartment())
                .active(user.isActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}