package com.servicedeskpro.dto.response;

import com.servicedeskpro.entity.enums.AuditAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private Long performedById;
    private String performedByFullName;
    private String performedByUsername;
    private AuditAction action;
    private String oldValue;
    private String newValue;
    private String notes;
    private Instant timestamp;
}