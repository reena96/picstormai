package com.rapidphoto.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of EmailService for development/testing.
 * Logs email content instead of actually sending emails.
 * Replace with real implementation (SMTP, AWS SES) for production.
 */
@Service
public class MockEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailService.class);
    private static final String BASE_URL = "http://localhost:8080"; // TODO: Configure from application.yml

    @Override
    public Mono<Void> sendVerificationEmail(String toEmail, String displayName, String verificationToken) {
        String verificationLink = BASE_URL + "/api/auth/verify-email?token=" + verificationToken;

        String emailContent = generateVerificationEmailHtml(displayName, verificationLink);

        logger.info("=== EMAIL VERIFICATION ===");
        logger.info("To: {}", toEmail);
        logger.info("Subject: Verify your RapidPhotoUpload email");
        logger.info("Verification Link: {}", verificationLink);
        logger.info("Token: {}", verificationToken);
        logger.info("========================");

        // In production, would send actual email here
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordResetEmail(String toEmail, String displayName, String resetToken) {
        String resetLink = BASE_URL + "/reset-password?token=" + resetToken;

        logger.info("=== PASSWORD RESET ===");
        logger.info("To: {}", toEmail);
        logger.info("Subject: Reset your RapidPhotoUpload password");
        logger.info("Reset Link: {}", resetLink);
        logger.info("=====================");

        return Mono.empty();
    }

    /**
     * Generates HTML email template for email verification.
     */
    private String generateVerificationEmailHtml(String displayName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button {
                        background-color: #4CAF50;
                        border: none;
                        color: white;
                        padding: 15px 32px;
                        text-align: center;
                        text-decoration: none;
                        display: inline-block;
                        font-size: 16px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Welcome to RapidPhotoUpload, %s!</h1>
                    <p>Thank you for registering. Please verify your email address to complete your registration.</p>
                    <p>Click the button below to verify your email:</p>
                    <a href="%s" class="button">Verify Email</a>
                    <p>Or copy and paste this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p class="footer">
                        This link will expire in 24 hours.<br>
                        If you did not create an account, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(displayName, verificationLink, verificationLink, verificationLink);
    }
}
