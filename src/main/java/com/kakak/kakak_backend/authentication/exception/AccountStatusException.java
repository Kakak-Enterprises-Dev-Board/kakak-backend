package com.kakak.kakak_backend.authentication.exception;

import org.springframework.http.HttpStatus;

public class AccountStatusException extends RuntimeException {
    private final String accountStatus;
    private final HttpStatus status;

    public AccountStatusException(String accountStatus, String message) {
        super(message);
        this.accountStatus = accountStatus;
        this.status = HttpStatus.FORBIDDEN;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
