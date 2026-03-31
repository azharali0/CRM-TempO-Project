package com.crm.dto.response;

import com.crm.model.entity.Customer;
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
public class CustomerDetailDTO {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String company;
    private String address;
    private String city;
    private String status;
    private UUID assignedToId;
    private String assignedToName;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime lastContactedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerDetailDTO fromEntity(Customer c) {
        return CustomerDetailDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .company(c.getCompany())
                .address(c.getAddress())
                .city(c.getCity())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .assignedToId(c.getAssignedTo() != null ? c.getAssignedTo().getId() : null)
                .assignedToName(c.getAssignedTo() != null ? c.getAssignedTo().getName() : null)
                .createdById(c.getCreatedBy() != null ? c.getCreatedBy().getId() : null)
                .createdByName(c.getCreatedBy() != null ? c.getCreatedBy().getName() : null)
                .lastContactedAt(c.getLastContactedAt())
                .version(c.getVersion())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
