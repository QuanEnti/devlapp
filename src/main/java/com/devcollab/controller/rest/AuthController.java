package com.devcollab.controller.rest;

import com.devcollab.domain.User;
import com.devcollab.dto.request.*;
import com.devcollab.dto.response.AuthResponseDTO;
import com.devcollab.dto.response.CheckEmailResponseDTO;
import com.devcollab.dto.response.ErrorResponseDTO;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.impl.core.UserServiceImpl;
import com.devcollab.service.system.JwtService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserServiceImpl userService;
    private final OtpService otpService;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@Valid @RequestBody CheckEmailRequestDTO request) {
        String email = request.getEmail();
        Optional<User> userOpt = userService.getByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(new CheckEmailResponseDTO("local", "REGISTER_NEW", null));
        }

        User user = userOpt.get();
        String provider = (user.getProvider() == null)
                ? "local"
                : user.getProvider().toLowerCase();

        switch (provider) {
            case "local_google":
                return ResponseEntity.ok(new CheckEmailResponseDTO("local_google", "PASSWORD_OPTIONAL", null));
            case "google":
                return ResponseEntity.ok(new CheckEmailResponseDTO("google", null, "/oauth2/authorization/google"));
            case "local":
                return ResponseEntity.ok(new CheckEmailResponseDTO("local", "PASSWORD_REQUIRED", null));
            case "otp":
                String otp = otpService.generateOtp();
                otpService.storeOtp(email, otp);
                mailService.sendOtpMail(email, otp);
                return ResponseEntity.ok(new CheckEmailResponseDTO("otp", "OTP_SENT", null));
            default:
                return ResponseEntity.ok(new CheckEmailResponseDTO("unknown", "UNKNOWN", null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        String email = request.getEmail();
        String password = request.getPassword();

        User user = userService.getByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponseDTO("NOT_FOUND", "Không tìm thấy tài khoản"));
        }

        if ("google".equalsIgnoreCase(user.getProvider())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_PROVIDER", "Tài khoản này chỉ có thể đăng nhập bằng Google."));
        }

        if (!userService.checkPassword(email, password)) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponseDTO("INVALID_PASSWORD", "Sai mật khẩu"));
        }

        if (!"verified".equalsIgnoreCase(user.getStatus())) {
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);
            mailService.sendOtpMail(email, otp);
            return ResponseEntity.ok(
                    new AuthResponseDTO("Mã OTP đã được gửi đến email để xác minh.",
                            "VERIFY_REQUIRED", user.getProvider(), null, email));
        }

        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(
                new AuthResponseDTO("Đăng nhập thành công", "SUCCESS", user.getProvider(), token, email));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_OTP", "OTP không hợp lệ hoặc đã hết hạn"));
        }

        userService.markVerified(email);
        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(
                new AuthResponseDTO("Xác minh email thành công", "SUCCESS", "local", token, email));
    }
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody ResendOtpRequestDTO request) {
        String email = request.getEmail();

        Optional<User> userOpt = userService.getByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("NOT_FOUND", "Không tìm thấy tài khoản với email này"));
        }

        if (otpService.isInCooldown(email, 15)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("COOLDOWN", "Vui lòng chờ 15 giây trước khi gửi lại OTP."));
        }

        String otp = otpService.generateOtp();
        otpService.storeOtp(email, otp);
        mailService.sendOtpMail(email, otp);

        log.info("OTP resent successfully to {}", email);
        return ResponseEntity.ok(
                new AuthResponseDTO("Đã gửi lại mã OTP đến email của bạn.", "OTP_RESENT", null, null, email));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        String email = request.getEmail();
        String password = request.getPassword();

        if (!isStrongPassword(password)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("WEAK_PASSWORD",
                            "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."));
        }

        Optional<User> existingOpt = userService.getByEmail(email);

        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            String provider = existing.getProvider().toLowerCase();

            if ("google".equals(provider)) {
                existing.setPasswordHash(userService.encodePassword(password));
                existing.setProvider("local_google");
                existing.setUpdatedAt(LocalDateTime.now());
                userService.save(existing);

                String otp = otpService.generateOtp();
                otpService.storeOtp(email, otp);
                mailService.sendOtpMail(email, otp);

                return ResponseEntity.ok(new AuthResponseDTO(
                        "Đã thêm mật khẩu cho tài khoản Google. OTP đã gửi đến email.",
                        "OTP_SENT", "local_google", null, email));
            }

            if ("local".equals(provider) || "local_google".equals(provider)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseDTO("EXISTED", "Email này đã có mật khẩu."));
            }

            if ("otp".equals(provider)) {
                existing.setPasswordHash(userService.encodePassword(password));
                existing.setProvider("local");
                existing.setUpdatedAt(LocalDateTime.now());
                userService.save(existing);

                String otp = otpService.generateOtp();
                otpService.storeOtp(email, otp);
                mailService.sendOtpMail(email, otp);

                return ResponseEntity.ok(new AuthResponseDTO(
                        "Đã thêm mật khẩu cho tài khoản OTP. Mã OTP đã được gửi đến email của bạn.",
                        "OTP_SENT", "local", null, email));
            }

            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("EXISTED", "Email đã tồn tại trong hệ thống"));
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(email.split("@")[0]);
        newUser.setPasswordHash(userService.encodePassword(password));
        newUser.setProvider("local");
        newUser.setStatus("unverified");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        newUser.setLastSeen(LocalDateTime.now());
        userService.save(newUser);

        String otp = otpService.generateOtp();
        otpService.storeOtp(email, otp);
        mailService.sendOtpMail(email, otp);

        return ResponseEntity.ok(new AuthResponseDTO(
                "Mã OTP đã được gửi đến email của bạn", "OTP_SENT", "local", null, email));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailOnlyDTO request) {
        String email = request.getEmail();

        Optional<User> userOpt = userService.getByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("NOT_FOUND", "Không tìm thấy tài khoản với email này"));
        }

        String otp = otpService.generateOtp();
        otpService.storeOtp(email, otp);
        mailService.sendOtpMail(email, otp);

        return ResponseEntity.ok(new AuthResponseDTO(
                "Mã OTP đã được gửi đến email của bạn.", "OTP_SENT", null, null, email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        String email = request.getEmail();
        String newPassword = request.getNewPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + email));

        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("WEAK_PASSWORD",
                            "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."));
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("DUPLICATE_PASSWORD",
                            "Mật khẩu mới không được trùng với mật khẩu cũ."));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("[UserService] Password reset successfully for {}", email);
        return ResponseEntity.ok(new AuthResponseDTO(
                "Đặt lại mật khẩu thành công", "SUCCESS", "local", null, email));
    }

    private boolean isStrongPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }
}
