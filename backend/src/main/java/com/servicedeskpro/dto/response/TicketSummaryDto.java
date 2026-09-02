package com.servicedeskpro.dto.response;

import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryDto {
    private Long id;
    private String ticketNumber;
    private String title;
    private TicketStatus status;
    private TicketPriority priority;
    private String categoryName;
    private String createdByFullName;
    private String createdByUsername;
    private String assignedToFullName;
    private String assignedToUsername;
    private Instant slaDueAt;
    private Instant createdAt;
    private Instant updatedAt;
}