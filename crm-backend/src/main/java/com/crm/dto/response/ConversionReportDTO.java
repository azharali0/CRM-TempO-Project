package com.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionReportDTO {

    private Map<String, Long> leadsPerStage;
    private BigDecimal winRate;
    private BigDecimal averageDealSize;
    private BigDecimal averageDaysToClose;
}
