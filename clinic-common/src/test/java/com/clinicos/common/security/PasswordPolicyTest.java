package com.clinicos.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void rejectsShortPasswords() {
        assertThat(PasswordPolicy.problem("abc12345", "a@b.com")).hasValueSatisfying(m -> assertThat(m).contains("10 characters"));
    }

    @Test
    void requiresLettersAndNumbers() {
        assertThat(PasswordPolicy.problem("onlyletterspass", null)).hasValueSatisfying(m -> assertThat(m).contains("letters and numbers"));
        assertThat(PasswordPolicy.problem("12345678901", null)).hasValueSatisfying(m -> assertThat(m).contains("letters and numbers"));
    }

    @Test
    void rejectsCommonPasswordsIgnoringCase() {
        assertThat(PasswordPolicy.problem("Password123", null)).hasValueSatisfying(m -> assertThat(m).contains("too common"));
        assertThat(PasswordPolicy.problem("Doctor@123", null)).hasValueSatisfying(m -> assertThat(m).contains("too common"));
    }

    @Test
    void rejectsPasswordsContainingTheEmailName() {
        assertThat(PasswordPolicy.problem("drmehta2026x", "dr.mehta@example.com")).hasValueSatisfying(m -> assertThat(m).contains("email name"));
    }

    @Test
    void acceptsAReasonablePassword() {
        assertThat(PasswordPolicy.problem("Teal-river-42", "doctor@example.com")).isEmpty();
    }
}
