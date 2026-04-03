package com.finance.dto.request;

import com.finance.enums.Role;
import com.finance.enums.UserStatus;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * All fields are optional â€” only non-null fields will be applied during update (PATCH semantics).
 */
@Data
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, digits, and underscores")
    private String username;

    @Email(message = "Must be a valid email address")
    private String email;

    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    private Role role;

    private UserStatus status;
}

