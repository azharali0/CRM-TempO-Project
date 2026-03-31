package com.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private long totalCustomers;
    private long openLeads;
    private long tasksDueToday;
    private long tasksOverdue;
    private BigDecimal totalRevenue;
    private LocalDateTime lastUpdatedAt;
}
