package com.finance.service;

import com.finance.dto.request.UserUpdateRequest;
import com.finance.dto.response.PagedResponse;
import com.finance.dto.response.UserResponse;
import com.finance.entity.User;
import com.finance.enums.UserStatus;
import com.finance.exception.BadRequestException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FinancialRecordRepository recordRepository;

    // â”€â”€â”€ List Users (paginated) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> result = userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
        return PagedResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsersByStatus(UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> result = userRepository.findAllByStatus(status, pageable)
                .map(UserResponse::fromEntity);
        return PagedResponse.from(result);
    }

    // â”€â”€â”€ Get Single User â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return UserResponse.fromEntity(user);
    }

    // â”€â”€â”€ Update User â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Partial update (PATCH semantics): only non-null fields are applied.
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUserById(id);

        if (StringUtils.hasText(request.getUsername())
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User saved = userRepository.save(user);
        log.info("User updated: id={}", id);
        return UserResponse.fromEntity(saved);
    }

    // â”€â”€â”€ Toggle Status â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Transactional
    public UserResponse setUserStatus(Long id, UserStatus status) {
        User user = findUserById(id);
        user.setStatus(status);
        User saved = userRepository.save(user);
        log.info("User status set to {} for id={}", status, id);
        return UserResponse.fromEntity(saved);
    }

    // â”€â”€â”€ Delete User â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Deletes a user permanently.
     * Any financial records they created have their createdBy field set to null
     * before deletion to avoid a FK constraint violation.
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserById(id);
        // Nullify FK references in financial_records before deleting the user
        recordRepository.nullifyCreatedBy(id);
        userRepository.delete(user);
        log.info("User deleted: id={}", id);
    }

    // â”€â”€â”€ Helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
