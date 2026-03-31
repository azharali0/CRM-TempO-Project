package com.crm.dto.request;

import com.crm.model.enums.LeadStage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageChangeRequest {

    @NotNull(message = "New stage is required")
    private LeadStage newStage;

    private String lostReason;
}
