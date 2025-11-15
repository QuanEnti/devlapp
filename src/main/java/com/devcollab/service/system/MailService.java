package com.devcollab.service.system;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.devcollab.domain.Project;
import com.devcollab.domain.User;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.otp.sender:no-reply@devcollab.local}")
    private String senderAddress;

    @Value("${app.base-url:http://localhost:8082}")
    private String baseUrl;

    // ======================================================
    // 🔑 Gửi OTP xác thực (text)
    // ======================================================
    @Async("mailExecutor")
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

            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
        }
    }

    // ======================================================
    // 🔔 Gửi Email Notification (đơn lẻ)
    // ======================================================
    @Async("mailExecutor")
    public void sendNotificationMail(String to, String title, String messageBody, String link,
            String senderName) {
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
            ctx.setVariable("link", baseUrl + (link != null ? link : ""));
            ctx.setVariable("baseUrl", baseUrl);
            ctx.setVariable("senderName", senderName != null ? senderName : "DevCollab System");
            ctx.setVariable("year", Year.now().getValue());

            String html = templateEngine.process("mail/notification.html", ctx);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Notification email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send Notification email to {}: {}", to, e.getMessage(), e);
        }
    }

    // ======================================================
    // 📬 Gửi Email Digest (tổng hợp nhiều thông báo)
    // ======================================================
    @Async("mailExecutor")
    public void sendDigestMail(String to, String title, List<Map<String, String>> notifications,
            String senderName) {
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
            ctx.setVariable("baseUrl", baseUrl);
            ctx.setVariable("senderName", senderName != null ? senderName : "DevCollab Digest");
            ctx.setVariable("year", Year.now().getValue());

            String html = templateEngine.process("mail/notification.html", ctx);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Digest email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send Digest email to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async("mailExecutor")
    public void sendInviteRegistrationMail(String to, Project project, User inviter, String token) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(senderAddress);
            helper.setTo(to);
            helper.setSubject("🚀 DevCollab – Lời mời tham gia dự án " + project.getName());

            Context ctx = new Context();
            ctx.setVariable("inviterName", inviter.getName());
            ctx.setVariable("projectName", project.getName());
            ctx.setVariable("registerLink", baseUrl + "/view/register?inviteToken=" + token);
            ctx.setVariable("year", Year.now().getValue());

            String html = templateEngine.process("mail/invite_register.html", ctx);
            helper.setText(html, true);

            mailSender.send(mime);
            log.info("Invite registration email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send invite registration email to {}: {}", to, e.getMessage(), e);
        }
    }

}
