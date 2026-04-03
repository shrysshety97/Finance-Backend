package com.finance.service;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RegisterRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.exception.BadRequestException;
import com.finance.repository.UserRepository;
import com.finance.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setRole(Role.ANALYST);

        savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.ANALYST)
                .build();
    }

    // â”€â”€â”€ Register â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("register: success - new user is saved and JWT is returned")
    void register_success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken("testuser")).thenReturn("jwt.token.value");

        AuthResponse result = authService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt.token.value");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register: throws BadRequestException when username is already taken")
    void register_duplicateUsername_throwsBadRequest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws BadRequestException when email is already registered")
    void register_duplicateEmail_throwsBadRequest() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
    }

    // â”€â”€â”€ Login â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("login: success - valid credentials return JWT")
    void login_success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken("testuser", "password123");

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateToken(any(
                org.springframework.security.core.Authentication.class)))
                .thenReturn("jwt.token.value");

        AuthResponse result = authService.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("jwt.token.value");
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("login: throws BadCredentialsException for wrong password")
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}
