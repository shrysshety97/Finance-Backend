package com.finance.service;

import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RegisterRequest;
import com.finance.dto.response.AuthResponse;
import com.finance.entity.User;
import com.finance.exception.BadRequestException;
import com.finance.repository.UserRepository;
import com.finance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user in the system.
     * Only an ADMIN is expected to register other users through the API.
     * However, the /register endpoint is public to allow seeding the first admin.
     *
     * Assumption: The first registered user can be made ADMIN via this endpoint.
     * In a production system, self-registration would default to VIEWER role.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        // Capture the returned entity â€” it carries the DB-generated ID
        User saved = userRepository.save(user);
        log.info("New user registered: username={}, role={}", saved.getUsername(), saved.getRole());

        String token = jwtTokenProvider.generateToken(saved.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .role(saved.getRole())
                .build();
    }

    /**
     * Authenticates a user and returns a JWT token.
     * Spring Security's AuthenticationManager handles credential verification
     * and throws BadCredentialsException / DisabledException on failure.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        log.info("User logged in: username={}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
