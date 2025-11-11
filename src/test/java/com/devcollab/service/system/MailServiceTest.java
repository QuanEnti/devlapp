package com.devcollab.service.system;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

/**
 * ✅ Unit Test cho MailService (chỉ test sendOtpMail) - Không còn lỗi field 'fromAddress' - Mock
 * JavaMailSender hoàn toàn - Không cần TemplateEngine vì không dùng trong OTP
 */
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine; // vẫn giữ để inject constructor

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // ✅ Gán giá trị hợp lệ cho senderAddress
        ReflectionTestUtils.setField(mailService, "senderAddress", "noreply@devcollab.local");
    }

    // ======================================================
    // ✅ TC01 - Gửi OTP Mail thành công
    // ======================================================
    @Test
    void testSendOtpMail_ShouldSendSuccessfully() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        mailService.sendOtpMail("user@example.com", "123456");

        // Assert
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // ======================================================
    // ✅ TC02 - Gửi OTP Mail bị lỗi (không throw exception ra ngoài)
    // ======================================================
    @Test
    void testSendOtpMail_ShouldHandleExceptionGracefully() throws Exception {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Act
        mailService.sendOtpMail("fail@example.com", "999999");

        // Assert
        verify(mailSender, never()).send(any(MimeMessage.class)); // Không gửi nếu lỗi
    }
}
