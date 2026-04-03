package com.finance.service;

import com.finance.dto.response.DashboardSummaryResponse;
import com.finance.enums.RecordType;
import com.finance.repository.FinancialRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock private FinancialRecordRepository recordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getSummary: computes net balance correctly")
    void getSummary_correctNetBalance() {
        when(recordRepository.sumByType(RecordType.INCOME))
                .thenReturn(new BigDecimal("50000.00"));
        when(recordRepository.sumByType(RecordType.EXPENSE))
                .thenReturn(new BigDecimal("35000.00"));
        when(recordRepository.countByDeletedFalse()).thenReturn(20L);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getTotalIncome()).isEqualByComparingTo("50000.00");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("35000.00");
        assertThat(result.getNetBalance()).isEqualByComparingTo("15000.00");
        assertThat(result.getTotalRecords()).isEqualTo(20L);
    }

    @Test
    @DisplayName("getSummary: net balance is negative when expenses exceed income")
    void getSummary_negativeBalance() {
        when(recordRepository.sumByType(RecordType.INCOME))
                .thenReturn(new BigDecimal("10000.00"));
        when(recordRepository.sumByType(RecordType.EXPENSE))
                .thenReturn(new BigDecimal("15000.00"));
        when(recordRepository.countByDeletedFalse()).thenReturn(5L);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.getNetBalance()).isEqualByComparingTo("-5000.00");
    }

    @Test
    @DisplayName("getCategoryTotals: returns empty list when no records exist")
    void getCategoryTotals_empty() {
        when(recordRepository.findCategoryWiseTotals()).thenReturn(Collections.emptyList());

        List<com.finance.dto.response.CategoryTotalResponse> result = dashboardService.getCategoryTotals();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRecentActivity: limits result to max 50 records")
    void getRecentActivity_enforcesMaxLimit() {
        when(recordRepository.findRecentActivity(any())).thenReturn(Collections.emptyList());

        // Requesting 200, service should cap it at 50
        dashboardService.getRecentActivity(200);

        // Verify PageRequest was called with size <= 50
        verify(recordRepository).findRecentActivity(
                argThat(p -> p.getPageSize() <= 50));
    }
}
