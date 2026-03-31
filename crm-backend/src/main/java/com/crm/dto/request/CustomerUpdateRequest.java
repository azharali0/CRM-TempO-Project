package com.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateRequest {

    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 150, message = "Company must not exceed 150 characters")
    private String company;

    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    private UUID assignedTo;

    @NotNull(message = "Version is required for optimistic locking")
    private Integer version;
}
