package com.finance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables automatic population of @CreatedDate and @LastModifiedDate
 * fields on JPA entities via Spring Data Auditing.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // No additional beans needed â€” @EnableJpaAuditing registers
    // the AuditingEntityListener automatically.
}
