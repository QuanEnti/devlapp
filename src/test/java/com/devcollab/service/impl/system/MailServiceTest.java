package com.devcollab.service.impl.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.devcollab.service.system.MailService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setup() {
        mailService = new MailService(mailSender);
        ReflectionTestUtils.setField(mailService, "senderAddress", "no-reply@devcollab.local");
    }

    // M01: sendOtpMail - gửi thành công
    @Test
    void sendOtpMail_success_sendsWithCorrectFields() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        assertDoesNotThrow(() -> mailService.sendOtpMail("user@example.com", "123456"));
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertTrue(msg.getFrom().contains("no-reply@devcollab.local"));
        assertTrue(msg.getTo()[0].contains("user@example.com"));
        assertTrue(msg.getSubject().contains("Verification Code"));
        assertTrue(msg.getText().contains("123456")); // body chứa OTP
    }

    // M02: sendOtpMail - SMTP lỗi (negative)
    @Test
    void sendOtpMail_mailException_loggedButNotThrown() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> mailService.sendOtpMail("user@example.com", "123456"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // M03: sendOtpMail - body chứa OTP null (boundary – gián tiếp test
    // buildOtpEmailBody)
    @Test
    void sendOtpMail_nullOtp_bodyContainsLiteralNull() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        assertDoesNotThrow(() -> mailService.sendOtpMail("user@example.com", null));
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertTrue(msg.getText().contains("null")); // template sẽ format "null"
    }

    // M04: sendOtpMail - gửi tới nhiều domain (positive – kiểm tra không throw)
    @Test
    void sendOtpMail_differentRecipientDomains_noError() {
        assertDoesNotThrow(() -> mailService.sendOtpMail("a@foo.com", "999999"));
        assertDoesNotThrow(() -> mailService.sendOtpMail("b@bar.org", "111111"));
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
