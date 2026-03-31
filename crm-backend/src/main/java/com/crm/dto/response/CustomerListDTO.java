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
public class CustomerListDTO {

    private UUID id;
    private String name;
    private String email;
    private String maskedPhone;
    private String company;
    private String status;
    private String assignedToName;
    private LocalDateTime createdAt;

    public static CustomerListDTO fromEntity(Customer c) {
        return CustomerListDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .email(c.getEmail())
                .maskedPhone(maskPhone(c.getPhone()))
                .company(c.getCompany())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .assignedToName(c.getAssignedTo() != null ? c.getAssignedTo().getName() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() >= 7) {
            return digits.substring(0, 4) + "***" + digits.substring(digits.length() - 4);
        }
        return "***";
    }
}
