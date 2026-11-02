package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A staff member's sign-in access, as the clinic owner sees it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAccessDto {
    private String email;
    private String fullName;
    private String role;
    private String roleLabel;
    private Long staffMemberId;
    /** INVITED (no sign-in yet), ACTIVE, or REVOKED */
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime lastLogin;
    /** Whether the invite email went out. */
    private Boolean emailSent;
    /**
     * The set-password link, returned only when it could not be emailed, so the owner can pass it
     * on (e.g. by WhatsApp). It is a one-time credential; never logged.
     */
    private String inviteLink;
}
