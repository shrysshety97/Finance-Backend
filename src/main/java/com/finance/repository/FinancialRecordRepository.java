package com.finance.repository;

import com.finance.entity.FinancialRecord;
import com.finance.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // Find non-deleted record by id
    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    // Paginated listing with optional filters (non-deleted only)
    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.deleted = false " +
           "  AND (:type IS NULL OR r.type = :type) " +
           "  AND (:category IS NULL OR LOWER(r.category) = LOWER(:category)) " +
           "  AND (:startDate IS NULL OR r.date >= :startDate) " +
           "  AND (:endDate IS NULL OR r.date <= :endDate) " +
           "ORDER BY r.date DESC, r.createdAt DESC")
    Page<FinancialRecord> findAllWithFilters(
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // Aggregate: total amount by type (non-deleted)
    @Query("SELECT COALESCE(SUM(r.amount), 0) " +
           "FROM FinancialRecord r " +
           "WHERE r.deleted = false AND r.type = :type")
    BigDecimal sumByType(@Param("type") RecordType type);

    // Aggregate: total amount by type within a date range
    @Query("SELECT COALESCE(SUM(r.amount), 0) " +
           "FROM FinancialRecord r " +
           "WHERE r.deleted = false " +
           "  AND r.type = :type " +
           "  AND r.date >= :startDate " +
           "  AND r.date <= :endDate")
    BigDecimal sumByTypeAndDateRange(
            @Param("type") RecordType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Category-wise totals grouped by type (non-deleted)
    @Query("SELECT r.category, r.type, SUM(r.amount) " +
           "FROM FinancialRecord r " +
           "WHERE r.deleted = false " +
           "GROUP BY r.category, r.type " +
           "ORDER BY SUM(r.amount) DESC")
    List<Object[]> findCategoryWiseTotals();

    // Monthly trends: year, month, type, total (non-deleted)
    @Query(value = "SELECT YEAR(r.date) AS year, " +
                   "       MONTH(r.date) AS month, " +
                   "       r.type, " +
                   "       SUM(r.amount) AS total " +
                   "FROM financial_records r " +
                   "WHERE r.deleted = false " +
                   "GROUP BY YEAR(r.date), MONTH(r.date), r.type " +
                   "ORDER BY year DESC, month DESC " +
                   "LIMIT :months", nativeQuery = true)
    List<Object[]> findMonthlyTrends(@Param("months") int months);

    // Recent activity (last N records, non-deleted)
    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.deleted = false " +
           "ORDER BY r.createdAt DESC")
    List<FinancialRecord> findRecentActivity(Pageable pageable);

    // Count non-deleted records
    long countByDeletedFalse();

    // Nullify FK reference when a user is deleted (prevents constraint violation)
    @Modifying
    @Query("UPDATE FinancialRecord r SET r.createdBy = null WHERE r.createdBy.id = :userId")
    void nullifyCreatedBy(@Param("userId") Long userId);

    // Full-text search: match keyword against category, notes, or type (non-deleted)
    @Query("SELECT r FROM FinancialRecord r " +
           "WHERE r.deleted = false " +
           "  AND (" +
           "    LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "    OR LOWER(r.notes)  LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "  ) " +
           "ORDER BY r.date DESC, r.createdAt DESC")
    Page<FinancialRecord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Weekly trends: ISO year-week grouping (non-deleted), MySQL YEARWEEK()
    @Query(value = "SELECT YEAR(r.date)               AS year, " +
                   "       WEEK(r.date, 1)            AS week, " +
                   "       r.type, " +
                   "       SUM(r.amount)              AS total, " +
                   "       MIN(r.date)                AS week_start " +
                   "FROM financial_records r " +
                   "WHERE r.deleted = false " +
                   "GROUP BY YEAR(r.date), WEEK(r.date, 1), r.type " +
                   "ORDER BY year DESC, week DESC " +
                   "LIMIT :weeks", nativeQuery = true)
    List<Object[]> findWeeklyTrends(@Param("weeks") int weeks);
}

