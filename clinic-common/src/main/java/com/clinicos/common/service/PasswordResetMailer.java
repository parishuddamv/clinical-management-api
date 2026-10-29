package com.clinicos.common.service;

/**
 * Delivers password reset links. The gateway provides an SMTP implementation; services without
 * one simply cannot send reset emails.
 */
public interface PasswordResetMailer {

    /** Sends the reset link. Implementations must not log the link, which works as a credential. */
    void sendResetLink(String email, String fullName, String resetUrl, int validMinutes);
}
