package com.clinicos.gateway.mail;

import com.clinicos.common.service.PasswordResetMailer;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/** Sends password reset links over SMTP. Active only when MAIL_HOST is set. */
@Component
@ConditionalOnExpression("'${spring.mail.host:}' != ''")
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private final JavaMailSender sender;
    private final String from;

    public SmtpPasswordResetMailer(JavaMailSender sender,
            @Value("${app.mail.from:CliniSys One <support@clinisysone.com>}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void sendResetLink(String email, String fullName, String resetUrl, int validMinutes) {
        String name = fullName == null || fullName.isBlank() ? "there" : fullName.trim().split("\\s+")[0];
        String text = "Hi " + name + ",\n\n"
                + "We received a request to reset the password for your CliniSys One account.\n\n"
                + "Choose a new password here (the link works once and expires in " + validMinutes + " minutes):\n"
                + resetUrl + "\n\n"
                + "If you did not ask for this, you can ignore this email. Your password will not change.\n\n"
                + "CliniSys One\nsupport@clinisysone.com";
        String safeUrl = HtmlUtils.htmlEscape(resetUrl);
        String html = "<p>Hi " + HtmlUtils.htmlEscape(name) + ",</p>"
                + "<p>We received a request to reset the password for your CliniSys One account.</p>"
                + "<p><a href=\"" + safeUrl + "\" style=\"display:inline-block;padding:10px 18px;background:#0f766e;color:#fff;"
                + "border-radius:6px;text-decoration:none;font-weight:600\">Choose a new password</a></p>"
                + "<p>The link works once and expires in " + validMinutes + " minutes. If the button does not work, copy this "
                + "address into your browser:<br><span style=\"word-break:break-all\">" + safeUrl + "</span></p>"
                + "<p>If you did not ask for this, you can ignore this email. Your password will not change.</p>"
                + "<p>CliniSys One<br>support@clinisysone.com</p>";
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Reset your CliniSys One password");
            helper.setText(text, html);
            sender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException("Reset email could not be built", e);
        }
    }
}
