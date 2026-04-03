package com.finance.dto.request;

import com.finance.enums.RecordType;
import javax.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinancialRecordRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Type is required (INCOME or EXPENSE)")
    private RecordType type;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Date must not be in the future")
    private LocalDate date;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
