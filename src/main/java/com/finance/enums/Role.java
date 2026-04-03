package com.finance.enums;

/**
 * Defines user roles within the Finance Dashboard system.
 *
 * VIEWER  - Read-only access to dashboard summary data.
 * ANALYST - Can view financial records and access detailed analytics/insights.
 * ADMIN   - Full access: manage users, records, and all dashboard features.
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}
