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
public class CommentDto {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String authorFullName;
    private String authorUsername;
    private String content;
    private boolean internalNote;
    private Instant createdAt;
    private Instant updatedAt;
}