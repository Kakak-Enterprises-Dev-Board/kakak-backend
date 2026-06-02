package com.kakak.kakak_backend.authentication.exception;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException() {
        super("Session not found or already revoked");
    }
}
