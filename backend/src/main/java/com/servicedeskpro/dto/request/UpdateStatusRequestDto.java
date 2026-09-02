package com.servicedeskpro.dto.request;

import com.servicedeskpro.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequestDto {

    @NotNull(message = "Target status is required")
    private TicketStatus status;

    private String notes;
}