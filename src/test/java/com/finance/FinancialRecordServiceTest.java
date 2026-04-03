package com.finance.service;

import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.entity.FinancialRecord;
import com.finance.entity.User;
import com.finance.enums.RecordType;
import com.finance.enums.Role;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialRecordService Tests")
class FinancialRecordServiceTest {

    @Mock private FinancialRecordRepository recordRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private FinancialRecordService recordService;

    private User adminUser;
    private FinancialRecord sampleRecord;
    private FinancialRecordRequest recordRequest;

    @BeforeEach
    void setUp() {
        // Simulate authenticated admin in SecurityContext
        adminUser = User.builder()
                .id(1L).username("admin").role(Role.ADMIN).build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        sampleRecord = FinancialRecord.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .type(RecordType.INCOME)
                .category("Salary")
                .date(LocalDate.of(2024, 1, 15))
                .notes("January salary")
                .createdBy(adminUser)
                .deleted(false)
                .build();

        recordRequest = new FinancialRecordRequest();
        recordRequest.setAmount(new BigDecimal("5000.00"));
        recordRequest.setType(RecordType.INCOME);
        recordRequest.setCategory("Salary");
        recordRequest.setDate(LocalDate.of(2024, 1, 15));
        recordRequest.setNotes("January salary");
    }

    // â”€â”€â”€ Create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("createRecord: persists and returns new record")
    void createRecord_success() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(sampleRecord);

        FinancialRecordResponse result = recordService.createRecord(recordRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getType()).isEqualTo(RecordType.INCOME);
        assertThat(result.getCategory()).isEqualTo("Salary");
        verify(recordRepository).save(any(FinancialRecord.class));
    }

    // â”€â”€â”€ Read â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("getRecordById: returns record for valid id")
    void getRecordById_found() {
        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));

        FinancialRecordResponse result = recordService.getRecordById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategory()).isEqualTo("Salary");
    }

    @Test
    @DisplayName("getRecordById: throws ResourceNotFoundException for missing id")
    void getRecordById_notFound() {
        when(recordRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getRecordById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // â”€â”€â”€ Update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("updateRecord: applies changes and returns updated record")
    void updateRecord_success() {
        recordRequest.setAmount(new BigDecimal("6000.00"));
        recordRequest.setCategory("Bonus");

        FinancialRecord updatedRecord = FinancialRecord.builder()
                .id(1L)
                .amount(new BigDecimal("6000.00"))
                .type(RecordType.INCOME)
                .category("Bonus")
                .date(LocalDate.of(2024, 1, 15))
                .createdBy(adminUser)
                .deleted(false)
                .build();

        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(updatedRecord);

        FinancialRecordResponse result = recordService.updateRecord(1L, recordRequest);

        assertThat(result.getAmount()).isEqualByComparingTo("6000.00");
        assertThat(result.getCategory()).isEqualTo("Bonus");
    }

    // â”€â”€â”€ Soft Delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("deleteRecord: sets deleted=true (soft delete)")
    void deleteRecord_softDelete() {
        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        recordService.deleteRecord(1L);

        assertThat(sampleRecord.isDeleted()).isTrue();
        verify(recordRepository).save(sampleRecord);
    }

    @Test
    @DisplayName("deleteRecord: throws ResourceNotFoundException for already-deleted record")
    void deleteRecord_alreadyDeleted() {
        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteRecord(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
