package com.servicedeskpro.controller;

import com.servicedeskpro.dto.request.*;
import com.servicedeskpro.dto.response.ApiResponse;
import com.servicedeskpro.dto.response.PageResponse;
import com.servicedeskpro.dto.response.TicketDetailDto;
import com.servicedeskpro.dto.response.TicketSummaryDto;
import com.servicedeskpro.entity.enums.TicketPriority;
import com.servicedeskpro.entity.enums.TicketStatus;
import com.servicedeskpro.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Ticket Management", description = "Endpoints for managing the IT ticket lifecycle, search, filtering, and pagination")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Create Support Ticket", description = "Creates a new IT support ticket with SLA target calculation")
    public ResponseEntity<ApiResponse<TicketDetailDto>> createTicket(@Valid @RequestBody CreateTicketRequestDto request) {
        TicketDetailDto created = ticketService.createTicket(request);
        return new ResponseEntity<>(ApiResponse.success(created, "Ticket created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Search, Filter & Paginate Tickets", description = "Dynamically queries tickets by status, priority, category, agent, or search keyword")
    public ResponseEntity<ApiResponse<PageResponse<TicketSummaryDto>>> getTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean myTickets,
            @RequestParam(required = false) Boolean unassigned) {

        PageResponse<TicketSummaryDto> response = ticketService.getTickets(
                page, size, sortBy, sortDir, status, priority, categoryId, assignedToId, createdById, search, myTickets, unassigned
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Ticket by ID", description = "Retrieves full details of a specific ticket (ownership checked)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> getTicketById(@PathVariable Long id) {
        TicketDetailDto ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Ticket Details", description = "Updates ticket title, description, category, or priority (OPEN state only)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> updateTicket(@PathVariable Long id,
                                                                     @Valid @RequestBody UpdateTicketRequestDto request) {
        TicketDetailDto updated = ticketService.updateTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Ticket Status", description = "Transitions ticket status (Agent & Admin only)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> updateStatus(@PathVariable Long id,
                                                                     @Valid @RequestBody UpdateStatusRequestDto request) {
        TicketDetailDto updated = ticketService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Status updated successfully"));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign Ticket", description = "Assigns ticket to an agent (Self-assign or Admin reassignment)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> assignTicket(@PathVariable Long id,
                                                                     @RequestBody(required = false) AssignTicketRequestDto request) {
        AssignTicketRequestDto req = request != null ? request : new AssignTicketRequestDto();
        TicketDetailDto updated = ticketService.assignTicket(id, req);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket assigned successfully"));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve Ticket", description = "Resolves ticket with mandatory resolution notes (Agent & Admin only)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> resolveTicket(@PathVariable Long id,
                                                                      @Valid @RequestBody ResolveTicketRequestDto request) {
        TicketDetailDto updated = ticketService.resolveTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket resolved successfully"));
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Reopen Resolved Ticket", description = "Reopens a resolved ticket with reason (Creator & Admin only)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> reopenTicket(@PathVariable Long id,
                                                                     @Valid @RequestBody ReopenTicketRequestDto request) {
        TicketDetailDto updated = ticketService.reopenTicket(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket reopened successfully"));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Confirm Resolution & Close Ticket", description = "Confirms resolution and closes ticket (Creator & Admin only)")
    public ResponseEntity<ApiResponse<TicketDetailDto>> closeTicket(@PathVariable Long id) {
        TicketDetailDto updated = ticketService.closeTicket(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket closed successfully"));
    }
}