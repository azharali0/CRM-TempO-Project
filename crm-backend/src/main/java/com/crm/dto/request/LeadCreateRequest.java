package com.crm.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadCreateRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @DecimalMin(value = "0.00", message = "Value must be at least 0.00")
    private BigDecimal value;

    private LocalDate expectedCloseDate;

    @Min(value = 0, message = "Probability must be at least 0")
    @Max(value = 100, message = "Probability must not exceed 100")
    private Integer probability;
}
