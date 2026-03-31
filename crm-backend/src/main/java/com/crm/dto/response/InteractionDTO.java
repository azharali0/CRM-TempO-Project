package com.crm.dto.response;

import com.crm.model.entity.Interaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionDTO {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private String type;
    private String subject;
    private String notes;
    private Integer duration;
    private String loggedByName;
    private UUID loggedById;
    private LocalDateTime createdAt;

    public static InteractionDTO fromEntity(Interaction interaction) {
        return InteractionDTO.builder()
                .id(interaction.getId())
                .customerId(interaction.getCustomer() != null ? interaction.getCustomer().getId() : null)
                .customerName(interaction.getCustomer() != null ? interaction.getCustomer().getName() : null)
                .type(interaction.getType() != null ? interaction.getType().name() : null)
                .subject(interaction.getSubject())
                .notes(interaction.getNotes())
                .duration(interaction.getDuration())
                .loggedByName(interaction.getLoggedBy() != null ? interaction.getLoggedBy().getName() : null)
                .loggedById(interaction.getLoggedBy() != null ? interaction.getLoggedBy().getId() : null)
                .createdAt(interaction.getCreatedAt())
                .build();
    }
}
