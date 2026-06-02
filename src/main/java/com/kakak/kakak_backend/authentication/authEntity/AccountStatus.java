package com.kakak.kakak_backend.authentication.authEntity;

import java.util.Locale;

public enum AccountStatus {
    ACTIVE,
    NOT_ACTIVATED,
    IN_PROGRESS,
    SUSPENDED,
    UNKNOWN;

    public static AccountStatus from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        try {
            return AccountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
