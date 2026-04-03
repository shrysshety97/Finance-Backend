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
public class CategoryTotalResponse {

    private String category;
    private RecordType type;
    private BigDecimal total;
}
