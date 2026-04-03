package com.finance.service;

import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.dto.response.PagedResponse;
import com.finance.entity.FinancialRecord;
import com.finance.entity.User;
import com.finance.enums.RecordType;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;

    // â”€â”€â”€ Create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public FinancialRecordResponse createRecord(FinancialRecordRequest request) {
        User currentUser = getCurrentUser();

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                .createdBy(currentUser)
                .build();

        FinancialRecord saved = recordRepository.save(record);
        log.info("Financial record created: id={}, type={}, amount={}", saved.getId(), saved.getType(), saved.getAmount());
        return FinancialRecordResponse.fromEntity(saved);
    }

    // â”€â”€â”€ Read (paginated + filtered) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> getRecords(
            RecordType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<FinancialRecordResponse> result = recordRepository
                .findAllWithFilters(type, category, startDate, endDate, pageable)
                .map(FinancialRecordResponse::fromEntity);

        return PagedResponse.from(result);
    }

    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        FinancialRecord record = findActiveRecord(id);
        return FinancialRecordResponse.fromEntity(record);
    }

    // â”€â”€â”€ Update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, FinancialRecordRequest request) {
        FinancialRecord record = findActiveRecord(id);

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory().trim());
        record.setDate(request.getDate());
        record.setNotes(request.getNotes());

        FinancialRecord saved = recordRepository.save(record);
        log.info("Financial record updated: id={}", id);
        return FinancialRecordResponse.fromEntity(saved);
    }

    // â”€â”€â”€ Soft Delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Soft delete: marks the record as deleted without removing it from the database.
     * This preserves historical data integrity and allows auditing.
     */
    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findActiveRecord(id);
        record.setDeleted(true);
        recordRepository.save(record);
        log.info("Financial record soft-deleted: id={}", id);
    }

    // â”€â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Full-text keyword search across category and notes fields.
     * Case-insensitive, paginated.
     */
    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> searchRecords(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FinancialRecordResponse> result = recordRepository
                .searchByKeyword(keyword.trim(), pageable)
                .map(FinancialRecordResponse::fromEntity);
        return PagedResponse.from(result);
    }

    // â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private FinancialRecord findActiveRecord(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record", id));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
