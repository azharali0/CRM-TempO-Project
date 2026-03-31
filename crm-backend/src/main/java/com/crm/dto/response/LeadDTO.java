package com.crm.dto.response;

import com.crm.model.entity.Lead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadDTO {

    private UUID id;
    private String customerName;
    private UUID customerId;
    private String title;
    private String stage;
    private BigDecimal value;
    private LocalDate expectedCloseDate;
    private Integer probability;
    private String lostReason;
    private String ownerName;
    private UUID ownerId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LeadDTO fromEntity(Lead lead) {
        return LeadDTO.builder()
                .id(lead.getId())
                .customerName(lead.getCustomer() != null ? lead.getCustomer().getName() : null)
                .customerId(lead.getCustomer() != null ? lead.getCustomer().getId() : null)
                .title(lead.getTitle())
                .stage(lead.getStage() != null ? lead.getStage().name() : null)
                .value(lead.getValue())
                .expectedCloseDate(lead.getExpectedCloseDate())
                .probability(lead.getProbability())
                .lostReason(lead.getLostReason())
                .ownerName(lead.getOwner() != null ? lead.getOwner().getName() : null)
                .ownerId(lead.getOwner() != null ? lead.getOwner().getId() : null)
                .version(lead.getVersion())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}
