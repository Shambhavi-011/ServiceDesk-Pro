package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.*;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.TicketDetailDto;
import com.servicedeskpro.dto.response.TicketSummaryDto;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;

public interface TicketService {
    TicketDetailDto createTicket(CreateTicketRequestDto request);
    TicketDetailDto getTicketById(Long id);
    TicketDetailDto updateTicket(Long id, UpdateTicketRequestDto request);
    TicketDetailDto updateStatus(Long id, UpdateStatusRequestDto request);
    TicketDetailDto assignTicket(Long id, AssignTicketRequestDto request);
    TicketDetailDto resolveTicket(Long id, ResolveTicketRequestDto request);
    TicketDetailDto reopenTicket(Long id, ReopenTicketRequestDto request);
    TicketDetailDto closeTicket(Long id);
    PageResponse<TicketSummaryDto> getTickets(
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
            Boolean unassigned
    );
}