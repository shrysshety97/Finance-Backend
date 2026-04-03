package com.finance.controller;

import com.finance.dto.response.*;
import com.finance.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dashboard analytics endpoints.
 *
 * â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
 * â”‚ Endpoint                                     â”‚ Allowed Roles                â”‚
 * â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
 * â”‚ GET /api/dashboard/summary                   â”‚ ADMIN, ANALYST, VIEWER       â”‚
 * â”‚ GET /api/dashboard/category-totals           â”‚ ADMIN, ANALYST, VIEWER       â”‚
 * â”‚ GET /api/dashboard/monthly-trends            â”‚ ADMIN, ANALYST               â”‚
 * â”‚ GET /api/dashboard/recent-activity           â”‚ ADMIN, ANALYST               â”‚
 * â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
 *
 * VIEWER role can access summary and category totals (read-only aggregated data).
 * Detailed trend/activity data is restricted to ANALYST and ADMIN.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Overall financial summary: total income, expenses, net balance, record count.
     * Accessible to all authenticated roles.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved", summary));
    }

    /**
     * Category-wise totals broken down by INCOME / EXPENSE.
     * Accessible to all authenticated roles.
     */
    @GetMapping("/category-totals")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CategoryTotalResponse>>> getCategoryTotals() {
        List<CategoryTotalResponse> totals = dashboardService.getCategoryTotals();
        return ResponseEntity.ok(ApiResponse.success("Category totals retrieved", totals));
    }

    /**
     * Monthly income/expense trend data for charting.
     *
     * @param months Number of months to include (default 12, max enforced in service)
     */
    @GetMapping("/monthly-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getMonthlyTrends(
            @RequestParam(defaultValue = "12") int months) {

        List<MonthlyTrendResponse> trends = dashboardService.getMonthlyTrends(months);
        return ResponseEntity.ok(ApiResponse.success("Monthly trends retrieved", trends));
    }

    /**
     * Weekly income/expense trend data for charting.
     *
     * @param weeks Number of ISO weeks to include (default 12)
     */
    @GetMapping("/weekly-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<List<WeeklyTrendResponse>>> getWeeklyTrends(
            @RequestParam(defaultValue = "12") int weeks) {

        List<WeeklyTrendResponse> trends = dashboardService.getWeeklyTrends(weeks);
        return ResponseEntity.ok(ApiResponse.success("Weekly trends retrieved", trends));
    }

    /**
     * Latest N financial record entries for the activity feed.
     *
     * @param limit Number of recent records to return (default 10, max 50 enforced in service)
     */
    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<List<FinancialRecordResponse>>> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit) {

        List<FinancialRecordResponse> activity = dashboardService.getRecentActivity(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent activity retrieved", activity));
    }
}
