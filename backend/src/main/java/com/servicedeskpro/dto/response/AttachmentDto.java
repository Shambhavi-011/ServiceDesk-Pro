package com.servicedeskpro.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private Long id;
    private Long ticketId;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedByFullName;
    private String uploadedByUsername;
    private Instant createdAt;
}