package com.finance.service;

import com.finance.dto.response.CategoryTotalResponse;
import com.finance.dto.response.DashboardSummaryResponse;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.dto.response.MonthlyTrendResponse;
import com.finance.dto.response.WeeklyTrendResponse;
import com.finance.enums.RecordType;
import com.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FinancialRecordRepository recordRepository;

    // â”€â”€â”€ Summary â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns high-level financial summary:
     * total income, total expenses, net balance, and record count.
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        BigDecimal totalIncome   = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);
        long       totalRecords  = recordRepository.countByDeletedFalse();

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .totalRecords(totalRecords)
                .build();
    }

    // â”€â”€â”€ Category Totals â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns per-category, per-type totals.
     * Example: { category: "Rent", type: "EXPENSE", total: 15000.00 }
     */
    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> getCategoryTotals() {
        return recordRepository.findCategoryWiseTotals()
                .stream()
                .map(row -> CategoryTotalResponse.builder()
                        .category((String) row[0])
                        .type(RecordType.valueOf((String) row[1]))
                        .total((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Monthly Trends â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns income and expense totals grouped by year-month.
     * Defaults to the last 12 months of data.
     *
     * @param months number of distinct year-month groups to return (default 12)
     */
    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrends(int months) {
        int limit = months * 2; // Ã—2 because INCOME and EXPENSE are separate rows per month

        return recordRepository.findMonthlyTrends(limit)
                .stream()
                .map(row -> {
                    int year  = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    String typeName = (String) row[2];
                    BigDecimal total = (BigDecimal) row[3];

                    return MonthlyTrendResponse.builder()
                            .year(year)
                            .month(month)
                            .monthName(Month.of(month).name())
                            .type(RecordType.valueOf(typeName))
                            .total(total)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Weekly Trends â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns income and expense totals grouped by ISO week number.
     *
     * @param weeks number of distinct ISO weeks to return (default 12)
     */
    @Transactional(readOnly = true)
    public List<WeeklyTrendResponse> getWeeklyTrends(int weeks) {
        int limit = weeks * 2; // Ã—2 for INCOME and EXPENSE rows per week

        return recordRepository.findWeeklyTrends(limit)
                .stream()
                .map(row -> {
                    int year      = ((Number) row[0]).intValue();
                    int week      = ((Number) row[1]).intValue();
                    String type   = (String) row[2];
                    BigDecimal total = (BigDecimal) row[3];
                    // row[4] is week_start date string from MySQL
                    java.time.LocalDate weekStart = java.time.LocalDate.parse(row[4].toString());

                    return WeeklyTrendResponse.builder()
                            .year(year)
                            .week(week)
                            .weekStart(weekStart)
                            .type(RecordType.valueOf(type))
                            .total(total)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // â”€â”€â”€ Recent Activity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Returns the N most recently created financial records.
     *
     * @param limit number of records to return (default 10, max 50)
     */
    @Transactional(readOnly = true)
    public List<FinancialRecordResponse> getRecentActivity(int limit) {
        int safeLimit = Math.min(limit, 50); // Guard against excessively large requests
        return recordRepository
                .findRecentActivity(PageRequest.of(0, safeLimit))
                .stream()
                .map(FinancialRecordResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

