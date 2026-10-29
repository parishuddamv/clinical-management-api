package com.clinicos.common.security;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Password rules for email sign-in. The web app mirrors these in src/utils/passwordPolicy.ts;
 * keep both in step so people see the same message before and after submitting.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON = Set.of(
            "123456789", "1234567890", "12345678", "0123456789", "1234512345", "1111111111", "0000000000",
            "password", "password1", "password12", "password123", "password@123", "passw0rd", "p@ssw0rd",
            "qwerty123", "qwertyuiop", "qwerty1234", "abc123456", "abcd1234", "abcdefgh12", "iloveyou1",
            "admin123", "admin@123", "welcome123", "welcome@123", "letmein123", "india@123", "india123",
            "doctor123", "doctor@123", "clinic123", "clinic@123", "hospital123", "clinisys123", "clinisysone",
            "changeme123", "test12345", "test@12345");

    private PasswordPolicy() {
    }

    /** Returns the reason a password is not acceptable, or empty when it is. */
    public static Optional<String> problem(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("Choose a password with at least " + MIN_LENGTH + " characters.");
        }
        if (password.length() > MAX_LENGTH) {
            return Optional.of("Use " + MAX_LENGTH + " characters or fewer.");
        }
        if (!password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit)) {
            return Optional.of("Use both letters and numbers in your password.");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower) || lower.chars().distinct().count() == 1) {
            return Optional.of("This password is too common. Choose something harder to guess.");
        }
        if (email != null) {
            String name = email.trim().toLowerCase(Locale.ROOT).split("@", 2)[0].replaceAll("[^a-z0-9]", "");
            if (name.length() >= 4 && lower.replaceAll("[^a-z0-9]", "").contains(name)) {
                return Optional.of("Do not use your email name in your password.");
            }
        }
        return Optional.empty();
    }
}
