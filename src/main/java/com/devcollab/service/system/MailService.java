package com.devcollab.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.otp.sender:no-reply@devcollab.local}")
    private String senderAddress;

    public void sendOtpMail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderAddress);
            message.setTo(to);
            message.setSubject("DevCollab – Your One-Time Verification Code");
            message.setText(buildOtpEmailBody(otp));

            mailSender.send(message);
            System.out.println("✅ [MailService] OTP email sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("❌ [MailService] Failed to send OTP email to " + to);
            e.printStackTrace();
        }
    }

    private String buildOtpEmailBody(String otp) {
        return """
                Hello,

                Your DevCollab verification code is: %s

                This code is valid for 5 minutes.
                Please do not share this code with anyone.

                Best regards,
                DevCollab Security Team
                """.formatted(otp);
    }
}
