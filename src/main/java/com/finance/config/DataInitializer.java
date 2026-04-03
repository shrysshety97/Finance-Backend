package com.finance.config;

import com.finance.entity.User;
import com.finance.enums.Role;
import com.finance.enums.UserStatus;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with a default ADMIN user on first startup.
 *
 * Default credentials (change immediately after first login):
 *   username : admin
 *   password : admin123
 *
 * This bean is skipped in the "test" profile to avoid interfering with tests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@finance.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("========================================================");
            log.info("  Default ADMIN user created.");
            log.info("  Username : admin");
            log.info("  Password : admin123");
            log.info("  *** Change this password immediately after login! ***");
            log.info("========================================================");
        }

        if (!userRepository.existsByUsername("analyst")) {
            User analyst = User.builder()
                    .username("analyst")
                    .email("analyst@finance.com")
                    .password(passwordEncoder.encode("analyst123"))
                    .role(Role.ANALYST)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(analyst);
            log.info("Default ANALYST user created: username=analyst / password=analyst123");
        }

        if (!userRepository.existsByUsername("viewer")) {
            User viewer = User.builder()
                    .username("viewer")
                    .email("viewer@finance.com")
                    .password(passwordEncoder.encode("viewer123"))
                    .role(Role.VIEWER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(viewer);
            log.info("Default VIEWER user created: username=viewer / password=viewer123");
        }
    }
}
