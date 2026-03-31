package com.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesRepReportDTO {

    private String repName;
    private UUID repId;
    private long dealsWon;
    private BigDecimal totalValue;
    private BigDecimal winRate;
}
