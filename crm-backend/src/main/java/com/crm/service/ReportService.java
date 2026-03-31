package com.crm.service;

import com.crm.dto.response.*;
import com.crm.exception.AccessDeniedException;
import com.crm.model.entity.Lead;
import com.crm.model.entity.User;
import com.crm.model.enums.InteractionType;
import com.crm.model.enums.LeadStage;
import com.crm.model.enums.TaskStatus;
import com.crm.model.enums.UserRole;
import com.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;

    public ReportService(CustomerRepository customerRepository,
                         LeadRepository leadRepository,
                         TaskRepository taskRepository,
                         InteractionRepository interactionRepository,
                         UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard(User currentUser) {
        long totalCustomers = countCustomers(currentUser);
        long openLeads = countOpenLeads(currentUser);
        long tasksDueToday = countTasksDueToday(currentUser);
        long tasksOverdue = countTasksOverdue(currentUser);
        BigDecimal totalRevenue = sumWonRevenue(currentUser);

        return DashboardDTO.builder()
                .totalCustomers(totalCustomers)
                .openLeads(openLeads)
                .tasksDueToday(tasksDueToday)
                .tasksOverdue(tasksOverdue)
                .totalRevenue(totalRevenue)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversionReportDTO getConversionReport(User currentUser) {
        Map<String, Long> leadsPerStage = new LinkedHashMap<>();
        long totalLeads;

        if (currentUser.getRole() == UserRole.ADMIN) {
            for (LeadStage stage : LeadStage.values()) {
                leadsPerStage.put(stage.name(), leadRepository.countByStage(stage));
            }
            totalLeads = leadRepository.count();
        } else if (currentUser.getRole() == UserRole.MANAGER) {
            List<UUID> visibleIds = getVisibleUserIds(currentUser);
            for (LeadStage stage : LeadStage.values()) {
                leadsPerStage.put(stage.name(), leadRepository.countByOwnerIdInAndStage(visibleIds, stage));
            }
            totalLeads = leadRepository.countByOwnerIdIn(visibleIds);
        } else {
            UUID userId = currentUser.getId();
            for (LeadStage stage : LeadStage.values()) {
                leadsPerStage.put(stage.name(), leadRepository.countByOwnerIdAndStage(userId, stage));
            }
            totalLeads = leadRepository.countByOwnerId(userId);
        }

        long wonCount = leadsPerStage.getOrDefault(LeadStage.WON.name(), 0L);

        BigDecimal winRate;
        if (totalLeads == 0) {
            winRate = BigDecimal.ZERO;
        } else {
            winRate = BigDecimal.valueOf(wonCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLeads), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalRevenue = sumWonRevenue(currentUser);

        BigDecimal averageDealSize;
        if (wonCount == 0) {
            averageDealSize = BigDecimal.ZERO;
        } else {
            averageDealSize = totalRevenue.divide(BigDecimal.valueOf(wonCount), 2, RoundingMode.HALF_UP);
        }

        BigDecimal averageDaysToClose = calculateAverageDaysToClose(currentUser);

        return ConversionReportDTO.builder()
                .leadsPerStage(leadsPerStage)
                .winRate(winRate)
                .averageDealSize(averageDealSize)
                .averageDaysToClose(averageDaysToClose)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SalesRepReportDTO> getSalesByRep(User currentUser) {
        if (currentUser.getRole() == UserRole.SALES_REP) {
            throw new AccessDeniedException("Only MANAGER or ADMIN can access sales-by-rep report");
        }

        List<Object[]> wonData;
        if (currentUser.getRole() == UserRole.ADMIN) {
            wonData = leadRepository.findWonLeadsGroupedByOwner();
        } else {
            List<UUID> visibleIds = getVisibleUserIds(currentUser);
            wonData = leadRepository.findWonLeadsGroupedByOwnerIn(visibleIds);
        }

        List<SalesRepReportDTO> results = new ArrayList<>();
        for (Object[] row : wonData) {
            UUID repId = (UUID) row[0];
            String repName = (String) row[1];
            long dealsWon = (Long) row[2];
            BigDecimal totalValue = (BigDecimal) row[3];

            long totalLeadsByRep;
            if (currentUser.getRole() == UserRole.ADMIN) {
                totalLeadsByRep = leadRepository.countByOwnerId(repId);
            } else {
                totalLeadsByRep = leadRepository.countByOwnerId(repId);
            }

            BigDecimal repWinRate;
            if (totalLeadsByRep == 0) {
                repWinRate = BigDecimal.ZERO;
            } else {
                repWinRate = BigDecimal.valueOf(dealsWon)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalLeadsByRep), 2, RoundingMode.HALF_UP);
            }

            results.add(SalesRepReportDTO.builder()
                    .repId(repId)
                    .repName(repName)
                    .dealsWon(dealsWon)
                    .totalValue(totalValue)
                    .winRate(repWinRate)
                    .build());
        }

        results.sort((a, b) -> b.getTotalValue().compareTo(a.getTotalValue()));
        return results;
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendDTO> getMonthlyTrend(User currentUser) {
        LocalDateTime twelveMonthsAgo = YearMonth.now().minusMonths(11).atDay(1).atStartOfDay();

        List<Lead> wonLeads;
        List<Lead> allLeads;
        if (currentUser.getRole() == UserRole.ADMIN) {
            wonLeads = leadRepository.findByStageAndUpdatedAtAfter(LeadStage.WON, twelveMonthsAgo);
            allLeads = new ArrayList<>(wonLeads);
            for (LeadStage stage : LeadStage.values()) {
                if (stage != LeadStage.WON) {
                    allLeads.addAll(leadRepository.findByStageAndUpdatedAtAfter(stage, twelveMonthsAgo));
                }
            }
        } else if (currentUser.getRole() == UserRole.MANAGER) {
            List<UUID> visibleIds = getVisibleUserIds(currentUser);
            wonLeads = leadRepository.findByStageAndOwnerIdInAndUpdatedAtAfter(LeadStage.WON, visibleIds, twelveMonthsAgo);
            allLeads = new ArrayList<>(wonLeads);
            for (LeadStage stage : LeadStage.values()) {
                if (stage != LeadStage.WON) {
                    allLeads.addAll(leadRepository.findByStageAndOwnerIdInAndUpdatedAtAfter(stage, visibleIds, twelveMonthsAgo));
                }
            }
        } else {
            UUID userId = currentUser.getId();
            wonLeads = leadRepository.findByStageAndOwnerIdAndUpdatedAtAfter(LeadStage.WON, userId, twelveMonthsAgo);
            allLeads = new ArrayList<>(wonLeads);
            for (LeadStage stage : LeadStage.values()) {
                if (stage != LeadStage.WON) {
                    allLeads.addAll(leadRepository.findByStageAndOwnerIdAndUpdatedAtAfter(stage, userId, twelveMonthsAgo));
                }
            }
        }

        // Group WON leads by month for revenue
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        Map<String, Long> leadCountByMonth = new LinkedHashMap<>();

        // Initialize last 12 months
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            String monthKey = current.minusMonths(i).toString();
            revenueByMonth.put(monthKey, BigDecimal.ZERO);
            leadCountByMonth.put(monthKey, 0L);
        }

        for (Lead lead : wonLeads) {
            if (lead.getUpdatedAt() != null) {
                String monthKey = YearMonth.from(lead.getUpdatedAt()).toString();
                if (revenueByMonth.containsKey(monthKey)) {
                    BigDecimal val = lead.getValue() != null ? lead.getValue() : BigDecimal.ZERO;
                    revenueByMonth.merge(monthKey, val, BigDecimal::add);
                }
            }
        }

        // Use createdAt for lead counts (when the lead was created)
        for (Lead lead : allLeads) {
            if (lead.getCreatedAt() != null) {
                String monthKey = YearMonth.from(lead.getCreatedAt()).toString();
                if (leadCountByMonth.containsKey(monthKey)) {
                    leadCountByMonth.merge(monthKey, 1L, Long::sum);
                }
            }
        }

        List<MonthlyTrendDTO> trends = new ArrayList<>();
        for (String month : revenueByMonth.keySet()) {
            trends.add(MonthlyTrendDTO.builder()
                    .month(month)
                    .revenue(revenueByMonth.get(month))
                    .leadCount(leadCountByMonth.getOrDefault(month, 0L))
                    .build());
        }

        return trends;
    }

    @Transactional(readOnly = true)
    public ActivitySummaryDTO getActivitySummary(User currentUser) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long interactionsToday;
        long interactionsThisWeek;
        long interactionsThisMonth;
        Map<String, Long> byType;

        if (currentUser.getRole() == UserRole.ADMIN) {
            interactionsToday = interactionRepository.countByCreatedAtAfter(startOfToday);
            interactionsThisWeek = interactionRepository.countByCreatedAtAfter(startOfWeek);
            interactionsThisMonth = interactionRepository.countByCreatedAtAfter(startOfMonth);
            byType = buildByTypeMap(interactionRepository.countByTypeAfter(startOfMonth));
        } else if (currentUser.getRole() == UserRole.MANAGER) {
            List<UUID> visibleIds = getVisibleUserIds(currentUser);
            interactionsToday = interactionRepository.countByLoggedByIdInAndCreatedAtAfter(visibleIds, startOfToday);
            interactionsThisWeek = interactionRepository.countByLoggedByIdInAndCreatedAtAfter(visibleIds, startOfWeek);
            interactionsThisMonth = interactionRepository.countByLoggedByIdInAndCreatedAtAfter(visibleIds, startOfMonth);
            byType = buildByTypeMap(interactionRepository.countByTypeAndUserInAfter(visibleIds, startOfMonth));
        } else {
            UUID userId = currentUser.getId();
            interactionsToday = interactionRepository.countByLoggedByIdAndCreatedAtAfter(userId, startOfToday);
            interactionsThisWeek = interactionRepository.countByLoggedByIdAndCreatedAtAfter(userId, startOfWeek);
            interactionsThisMonth = interactionRepository.countByLoggedByIdAndCreatedAtAfter(userId, startOfMonth);
            byType = buildByTypeMap(interactionRepository.countByTypeAndUserAfter(userId, startOfMonth));
        }

        return ActivitySummaryDTO.builder()
                .interactionsToday(interactionsToday)
                .interactionsThisWeek(interactionsThisWeek)
                .interactionsThisMonth(interactionsThisMonth)
                .byType(byType)
                .build();
    }

    // --- Private helpers ---

    private long countCustomers(User currentUser) {
        return switch (currentUser.getRole()) {
            case ADMIN -> customerRepository.count();
            case MANAGER -> customerRepository.countByAssignedToIdIn(getVisibleUserIds(currentUser));
            case SALES_REP -> customerRepository.countByAssignedToId(currentUser.getId());
        };
    }

    private long countOpenLeads(User currentUser) {
        List<LeadStage> closedStages = List.of(LeadStage.WON, LeadStage.LOST);
        return switch (currentUser.getRole()) {
            case ADMIN -> leadRepository.countByStageNotIn(closedStages);
            case MANAGER -> leadRepository.countByOwnerIdInAndStageNotIn(getVisibleUserIds(currentUser), closedStages);
            case SALES_REP -> leadRepository.countByOwnerIdAndStageNotIn(currentUser.getId(), closedStages);
        };
    }

    private long countTasksDueToday(User currentUser) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<TaskStatus> activeStatuses = List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);

        return switch (currentUser.getRole()) {
            case ADMIN -> taskRepository.countDueToday(dayStart, dayEnd, activeStatuses);
            case MANAGER -> taskRepository.countDueTodayByUserIn(dayStart, dayEnd, activeStatuses, getVisibleUserIds(currentUser));
            case SALES_REP -> taskRepository.countDueTodayByUser(dayStart, dayEnd, activeStatuses, currentUser.getId());
        };
    }

    private long countTasksOverdue(User currentUser) {
        return switch (currentUser.getRole()) {
            case ADMIN -> taskRepository.countByStatus(TaskStatus.OVERDUE);
            case MANAGER -> taskRepository.countByAssignedToIdInAndStatus(getVisibleUserIds(currentUser), TaskStatus.OVERDUE);
            case SALES_REP -> taskRepository.countByAssignedToIdAndStatus(currentUser.getId(), TaskStatus.OVERDUE);
        };
    }

    private BigDecimal sumWonRevenue(User currentUser) {
        BigDecimal revenue = switch (currentUser.getRole()) {
            case ADMIN -> leadRepository.sumValueByStage(LeadStage.WON);
            case MANAGER -> leadRepository.sumValueByStageAndOwnerIdIn(LeadStage.WON, getVisibleUserIds(currentUser));
            case SALES_REP -> leadRepository.sumValueByStageAndOwnerId(LeadStage.WON, currentUser.getId());
        };
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    private BigDecimal calculateAverageDaysToClose(User currentUser) {
        List<Lead> wonLeads;
        if (currentUser.getRole() == UserRole.ADMIN) {
            wonLeads = leadRepository.findByStage(LeadStage.WON);
        } else if (currentUser.getRole() == UserRole.MANAGER) {
            wonLeads = leadRepository.findByStageAndOwnerIdIn(LeadStage.WON, getVisibleUserIds(currentUser));
        } else {
            wonLeads = leadRepository.findByStageAndOwnerId(LeadStage.WON, currentUser.getId());
        }

        if (wonLeads.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalDays = 0;
        int count = 0;
        for (Lead lead : wonLeads) {
            if (lead.getCreatedAt() != null && lead.getUpdatedAt() != null) {
                long days = ChronoUnit.DAYS.between(lead.getCreatedAt(), lead.getUpdatedAt());
                totalDays += days;
                count++;
            }
        }

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalDays)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Long> buildByTypeMap(List<Object[]> typeCountRows) {
        Map<String, Long> byType = new LinkedHashMap<>();
        for (InteractionType type : InteractionType.values()) {
            byType.put(type.name(), 0L);
        }
        for (Object[] row : typeCountRows) {
            InteractionType type = (InteractionType) row[0];
            Long count = (Long) row[1];
            byType.put(type.name(), count);
        }
        return byType;
    }

    private List<UUID> getVisibleUserIds(User currentUser) {
        List<UUID> visibleUserIds = new ArrayList<>();
        visibleUserIds.add(currentUser.getId());
        userRepository.findByRole(UserRole.SALES_REP)
                .forEach(u -> visibleUserIds.add(u.getId()));
        return visibleUserIds;
    }
}
