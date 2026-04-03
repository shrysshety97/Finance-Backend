package com.finance.dto.response;

import com.finance.enums.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendResponse {

    private int year;
    private int month;
    private String monthName;
    private RecordType type;
    private BigDecimal total;
}
