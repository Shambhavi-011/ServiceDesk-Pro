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
public class ResolveTicketRequestDto {

    @NotBlank(message = "Resolution notes are required when resolving a ticket")
    private String resolutionNotes;
}