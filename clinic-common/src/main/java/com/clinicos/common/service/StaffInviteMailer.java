package com.clinicos.common.service;

/** Delivers staff invitations. The gateway provides an SMTP implementation when mail is configured. */
public interface StaffInviteMailer {

    /** Sends the invite. Implementations must not log the link, which works as a credential. */
    void sendInvite(String email, String fullName, String clinicName, String roleLabel, String invitedBy, String url, int validDays);
}
