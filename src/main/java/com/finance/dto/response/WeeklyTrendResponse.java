package com.finance.dto.response;

import com.finance.enums.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTrendResponse {

    private int year;

    /** ISO week number (1â€“53) */
    private int week;

    /** The Monday that starts this ISO week */
    private LocalDate weekStart;

    private RecordType type;
    private BigDecimal total;
}
