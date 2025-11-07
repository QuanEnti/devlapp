package com.devcollab.service.system;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.otp.sender:no-reply@devcollab.local}")
    private String senderAddress;

    // ======================================================
    // 🔑 Gửi OTP xác thực (text)
    // ======================================================
    public void sendOtpMail(String to, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(to);
            helper.setSubject("DevCollab – Your One-Time Verification Code");

            String body = """
                    Hello,

                    Your DevCollab verification code is: %s

                    This code is valid for 5 minutes.
                    Please do not share this code with anyone.

                    Best regards,
                    DevCollab Security Team
                    """.formatted(otp);

            helper.setText(body, false);
            mailSender.send(mimeMessage);

            System.out.println("✅ [MailService] OTP email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("❌ [MailService] Failed to send OTP email to " + to);
            e.printStackTrace();
        }
    }

    // ======================================================
    // 🔔 Gửi Email Notification (đơn lẻ)
    // ======================================================
    public void sendNotificationMail(String to, String title, String messageBody, String link, String senderName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(to);
            helper.setSubject("🔔 DevCollab – " + title);

            Context ctx = new Context();
            ctx.setVariable("isDigest", false);
            ctx.setVariable("title", title);
            ctx.setVariable("message", messageBody);
            ctx.setVariable("link", "https://devcollab.app" + (link != null ? link : ""));
            ctx.setVariable("senderName", senderName != null ? senderName : "DevCollab System");
            ctx.setVariable("year", Year.now().getValue());

            String html = templateEngine.process("mail/notification.html", ctx);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            System.out.println("✅ [MailService] Notification email sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("❌ [MailService] Failed to send Notification email to " + to);
            e.printStackTrace();
        }
    }

    // ======================================================
    // 📬 Gửi Email Digest (tổng hợp nhiều thông báo)
    // ======================================================
    public void sendDigestMail(String to, String title, List<Map<String, String>> notifications, String senderName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(to);
            helper.setSubject("📬 DevCollab Digest – " + title);

            Context ctx = new Context();
            ctx.setVariable("isDigest", true);
            ctx.setVariable("title", title);
            ctx.setVariable("notifications", notifications);
            ctx.setVariable("senderName", senderName != null ? senderName : "DevCollab Digest");
            ctx.setVariable("year", Year.now().getValue());

            String html = templateEngine.process("mail/notification.html", ctx);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            System.out.println("✅ [MailService] Digest email sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("❌ [MailService] Failed to send Digest email to " + to);
            e.printStackTrace();
        }
    }
}
