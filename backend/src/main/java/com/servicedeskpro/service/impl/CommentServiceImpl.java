package com.servicedeskpro.service.impl;

import com.servicedeskpro.dto.request.CreateCommentRequestDto;
import com.servicedeskpro.dto.response.CommentDto;
import com.servicedeskpro.entity.Comment;
import com.servicedeskpro.entity.Notification;
import com.servicedeskpro.entity.Ticket;
import com.servicedeskpro.entity.User;
import com.servicedeskpro.entity.enums.RoleName;
import com.servicedeskpro.exception.ForbiddenException;
import com.servicedeskpro.exception.ResourceNotFoundException;
import com.servicedeskpro.repository.CommentRepository;
import com.servicedeskpro.repository.NotificationRepository;
import com.servicedeskpro.repository.TicketRepository;
import com.servicedeskpro.repository.UserRepository;
import com.servicedeskpro.security.UserPrincipal;
import com.servicedeskpro.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long ticketId, CreateCommentRequestDto request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAgent = isUserInRole(currentUser, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);
        boolean isCreator = ticket.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAgent && !isAdmin && !isCreator) {
            throw new ForbiddenException("You are not authorized to comment on this ticket");
        }

        // Only Agents and Admins can create internal notes
        boolean internalNote = request.isInternalNote();
        if (internalNote && !isAgent && !isAdmin) {
            throw new ForbiddenException("Only Support Agents and Admins can post internal notes");
        }

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(currentUser)
                .content(request.getContent())
                .internalNote(internalNote)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // Notify counterpart if not an internal note
        if (!internalNote) {
            if (isCreator && ticket.getAssignedTo() != null) {
                createNotification(ticket.getAssignedTo(), ticket,
                        "New Comment on Ticket " + ticket.getTicketNumber(),
                        String.format("%s replied: %s", currentUser.getFirstName(), truncate(request.getContent(), 60)));
            } else if (!isCreator) {
                createNotification(ticket.getCreatedBy(), ticket,
                        "Update on Ticket " + ticket.getTicketNumber(),
                        String.format("Support Agent %s commented on your ticket", currentUser.getFirstName()));
            }
        }

        return mapToDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAgent = isUserInRole(currentUser, RoleName.ROLE_SUPPORT_AGENT);
        boolean isAdmin = isUserInRole(currentUser, RoleName.ROLE_ADMIN);
        boolean isCreator = ticket.getCreatedBy().getId().equals(currentUser.getId());

        if (!isAgent && !isAdmin && !isCreator) {
            throw new ForbiddenException("You are not authorized to view comments for this ticket");
        }

        List<Comment> comments;
        if (isAgent || isAdmin) {
            // Agents and Admins see both public comments and internal notes
            comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        } else {
            // Employees only see public comments
            comments = commentRepository.findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        }

        return comments.stream().map(this::mapToDto).collect(Collectors.toList());
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

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private CommentDto mapToDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .authorId(comment.getAuthor().getId())
                .authorFullName(comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName())
                .authorUsername(comment.getAuthor().getUsername())
                .content(comment.getContent())
                .internalNote(comment.isInternalNote())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}