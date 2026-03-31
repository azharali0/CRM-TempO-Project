package com.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryDTO {

    private long interactionsToday;
    private long interactionsThisWeek;
    private long interactionsThisMonth;
    private Map<String, Long> byType;
}
