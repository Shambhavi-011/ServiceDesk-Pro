package com.servicedeskpro.controller;

import com.servicedeskpro.dto.request.CreateCommentRequestDto;
import com.servicedeskpro.dto.response.ApiResponse;
import com.servicedeskpro.dto.response.CommentDto;
import com.servicedeskpro.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Ticket Comments", description = "Endpoints for posting and reading threaded ticket discussions and internal notes")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add Comment to Ticket", description = "Adds a public comment or internal agent note to a ticket")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(@PathVariable Long ticketId,
                                                              @Valid @RequestBody CreateCommentRequestDto request) {
        CommentDto created = commentService.addComment(ticketId, request);
        return new ResponseEntity<>(ApiResponse.success(created, "Comment added successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get Ticket Comments", description = "Lists comments for a ticket (Internal notes filtered for employees)")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(@PathVariable Long ticketId) {
        List<CommentDto> comments = commentService.getCommentsForTicket(ticketId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}