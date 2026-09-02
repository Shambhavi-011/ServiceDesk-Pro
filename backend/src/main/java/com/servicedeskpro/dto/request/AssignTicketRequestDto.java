package com.servicedeskpro.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTicketRequestDto {
    private Long agentId; // Nullable if agent is self-assigning
    private String notes;
}