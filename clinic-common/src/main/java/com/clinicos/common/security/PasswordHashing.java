package com.clinicos.common.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** One bcrypt encoder for every service, including the gateway (which has no PasswordEncoder bean). */
public final class PasswordHashing {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordHashing() {
    }

    public static PasswordEncoder encoder() {
        return ENCODER;
    }
}
