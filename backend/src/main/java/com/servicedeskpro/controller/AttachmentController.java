package com.servicedeskpro.controller;

import com.servicedeskpro.dto.response.ApiResponse;
import com.servicedeskpro.dto.response.AttachmentDto;
import com.servicedeskpro.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Ticket Attachments", description = "Endpoints for uploading, listing, and securely downloading ticket file attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/api/tickets/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload File Attachment", description = "Uploads a file (max 10MB) to a support ticket")
    public ResponseEntity<ApiResponse<AttachmentDto>> uploadAttachment(@PathVariable Long ticketId,
                                                                       @RequestParam("file") MultipartFile file) {
        AttachmentDto attachment = attachmentService.uploadAttachment(ticketId, file);
        return new ResponseEntity<>(ApiResponse.success(attachment, "File uploaded successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/api/tickets/{ticketId}/attachments")
    @Operation(summary = "List Ticket Attachments", description = "Lists all file attachments uploaded for a ticket")
    public ResponseEntity<ApiResponse<List<AttachmentDto>>> getAttachments(@PathVariable Long ticketId) {
        List<AttachmentDto> attachments = attachmentService.getAttachmentsForTicket(ticketId);
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    @GetMapping("/api/attachments/{attachmentId}/download")
    @Operation(summary = "Download Attachment", description = "Streams the attached file content with ownership verification")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        AttachmentDto metadata = attachmentService.getAttachmentMetadata(attachmentId);
        Resource resource = attachmentService.downloadAttachment(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(resource);
    }
}