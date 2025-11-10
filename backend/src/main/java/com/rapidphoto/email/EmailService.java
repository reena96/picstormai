package com.rapidphoto.email;

import reactor.core.publisher.Mono;

/**
 * Service for sending emails.
 * Can be implemented with SMTP, AWS SES, SendGrid, etc.
 */
public interface EmailService {

    /**
     * Send email verification email to user.
     * @param toEmail Recipient email address
     * @param displayName Recipient name
     * @param verificationToken 32-char verification token
     * @return Mono that completes when email is sent
     */
    Mono<Void> sendVerificationEmail(String toEmail, String displayName, String verificationToken);

    /**
     * Send password reset email to user.
     * @param toEmail Recipient email address
     * @param displayName Recipient name
     * @param resetToken Password reset token
     * @return Mono that completes when email is sent
     */
    Mono<Void> sendPasswordResetEmail(String toEmail, String displayName, String resetToken);
}
