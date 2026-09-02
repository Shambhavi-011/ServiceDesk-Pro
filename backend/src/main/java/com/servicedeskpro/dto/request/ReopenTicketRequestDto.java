package com.servicedeskpro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReopenTicketRequestDto {

    @NotBlank(message = "Reason for reopening the ticket is mandatory")
    private String reason;
}