package com.servicedeskpro.dto.request;

import com.servicedeskpro.entity.enums.TicketPriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequestDto {

    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @Size(min = 10, message = "Description must provide at least 10 characters")
    private String description;

    private Long categoryId;

    private TicketPriority priority;
}