package com.crm.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionUpdateRequest {

    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    @Min(value = 0, message = "Duration must be at least 0")
    private Integer duration;
}
