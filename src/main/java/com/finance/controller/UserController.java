package com.finance.controller;

import com.finance.dto.request.UserUpdateRequest;
import com.finance.dto.response.ApiResponse;
import com.finance.dto.response.PagedResponse;
import com.finance.dto.response.UserResponse;
import com.finance.enums.UserStatus;
import com.finance.service.UserService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User management endpoints â€” restricted to ADMIN role.
 *
 * GET    /api/users                  â€” List all users (paginated)
 * GET    /api/users/{id}             â€” Get user by ID
 * GET    /api/users/me               â€” Get current authenticated user's profile
 * PATCH  /api/users/{id}             â€” Partial update of user fields
 * PATCH  /api/users/{id}/status      â€” Toggle user active/inactive
 * DELETE /api/users/{id}             â€” Delete user
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    UserStatus status) {

        PagedResponse<UserResponse> response = (status != null)
                ? userService.getUsersByStatus(status, page, size)
                : userService.getAllUsers(page, size);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    /**
     * Allows any authenticated user to view their own profile.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails userDetails) {

        UserResponse user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", user));
    }

    /**
     * Partial update of a user â€” only non-null fields are applied (PATCH semantics).
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> setUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {

        UserResponse updated = userService.setUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated to " + status, updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}

