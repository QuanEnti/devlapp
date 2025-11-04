package com.devcollab.controller.rest;

import com.devcollab.domain.Role;
import com.devcollab.domain.User;
import com.devcollab.dto.UserDTO;
import com.devcollab.dto.request.*;
import com.devcollab.dto.response.AuthResponseDTO;
import com.devcollab.dto.response.CheckEmailResponseDTO;
import com.devcollab.dto.response.ErrorResponseDTO;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.impl.core.UserServiceImpl;
import com.devcollab.service.system.AuthService;
import com.devcollab.service.system.JwtService;
import com.devcollab.service.system.MailService;
import com.devcollab.service.system.OtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map;

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
    private final AuthService authService;

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@Valid @RequestBody CheckEmailRequestDTO request) {
        String email = request.getEmail();
        Optional<User> userOpt = userService.getByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(new CheckEmailResponseDTO("local", "REGISTER_NEW", null));
        }

        User user = userOpt.get();
        String provider = (user.getProvider() == null) ? "local" : user.getProvider().toLowerCase();

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
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request,
            @RequestParam(value = "redirect", required = false) String redirect,
            jakarta.servlet.http.HttpServletRequest servletRequest) {

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
            return ResponseEntity.ok(new AuthResponseDTO(
                    "Mã OTP đã được gửi đến email để xác minh.",
                    "VERIFY_REQUIRED",
                    user.getProvider(), null, email, "/view/verify-otp"));
        }

        // ✅ Thiết lập SecurityContext (giữ login session chuẩn Spring Security)
        var userDetails = userService.loadUserByUsername(email);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        servletRequest.getSession(true).setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);

        // ✅ Truyền redirect để quay lại trang mời sau khi login
        return issueTokensAndRedirect(email, user.getProvider(), "Đăng nhập thành công", redirect);
    }

    private ResponseEntity<?> issueTokensAndRedirect(String email, String provider, String message, String redirect) {
        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken(email);

        ResponseCookie accessCookie = ResponseCookie.from("AUTH_TOKEN", accessToken)
                .httpOnly(true).secure(false).path("/").maxAge(15 * 60).sameSite("Lax").build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true).secure(false).path("/").maxAge(7 * 24 * 60 * 60).sameSite("Lax").build();

        String redirectPath = (redirect != null && !redirect.isEmpty()) ? redirect : "/view/home";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new AuthResponseDTO(message, "SUCCESS", provider, accessToken, email, redirectPath));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        String email = request.getEmail();
        String otp = request.getOtp();
        String mode = request.getMode();

        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("INVALID_OTP", "OTP không hợp lệ hoặc đã hết hạn"));
        }

        log.info("[AuthController] OTP verified for {} (mode: {})", email, mode);

        if ("reset".equalsIgnoreCase(mode)) {
            return ResponseEntity.ok(
                    new AuthResponseDTO("OTP xác minh thành công", "RESET_READY", null, null, email,
                            "/view/reset-password"));
        }

        userService.markVerified(email);
        var userDetails = userService.loadUserByUsername(email);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        servletRequest.getSession(true).setAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);

        return issueTokensAndRedirect(email, "local", "Xác minh email thành công");
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

            switch (provider) {
                case "google":
                    existing.setPasswordHash(userService.encodePassword(password));
                    existing.setProvider("local_google");
                    existing.setUpdatedAt(LocalDateTime.now());
                    userService.save(existing);
                    sendOtp(email, "Google");
                    return ResponseEntity.ok(new AuthResponseDTO(
                            "Đã thêm mật khẩu cho tài khoản Google. OTP đã gửi đến email.",
                            "OTP_SENT", "local_google", null, email, "/view/verify-otp"));
                case "local":
                case "local_google":
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDTO("EXISTED", "Email này đã có mật khẩu."));
                default:
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponseDTO("EXISTED", "Email đã tồn tại trong hệ thống."));
            }
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(email.split("@")[0]);
        newUser.setPasswordHash(userService.encodePassword(password));
        newUser.setProvider("otp");
        newUser.setStatus("unverified");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        newUser.setLastSeen(LocalDateTime.now());
        userService.save(newUser);

        sendOtp(email, "Register");
        return ResponseEntity.ok(new AuthResponseDTO(
                "Mã OTP đã được gửi đến email của bạn", "OTP_SENT", "otp", null, email, "/view/verify-otp"));
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody EmailOnlyDTO request) {
        String email = request.getEmail();

        Optional<User> userOpt = userService.getByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("NOT_FOUND", "Không tìm thấy tài khoản với email này"));
        }

        try {
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);
            mailService.sendOtpMail(email, otp);
            log.info("[AuthController] OTP resent to {}", email);

            return ResponseEntity.ok(
                    new AuthResponseDTO(
                            "Mã OTP mới đã được gửi đến email của bạn.",
                            "OTP_RESENT",
                            null,
                            null,
                            email,
                            "/view/verify-otp"));
        } catch (Exception e) {
            log.error(" resend-otp error for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponseDTO("SERVER_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailOnlyDTO request) {
        String email = request.getEmail();
        Optional<User> userOpt = userService.getByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDTO("NOT_FOUND", "Không tìm thấy tài khoản với email này"));
        }

        sendOtp(email, "Forgot Password");
        return ResponseEntity.ok(new AuthResponseDTO(
                "Mã OTP đã được gửi đến email của bạn.", "OTP_SENT", null, null, email, "/view/verify-otp"));
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

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("[AuthController]  Password reset successfully for {}", email);

        return ResponseEntity.ok(
                new AuthResponseDTO(
                        "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.",
                        "RESET_SUCCESS",
                        "local",
                        null,
                        email,
                        "/view/password-reset-success"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "REFRESH_TOKEN", defaultValue = "") String refreshToken) {
        try {
            if (refreshToken.isEmpty() || !jwtService.isValid(refreshToken)) {
                return ResponseEntity.status(401)
                        .body(new ErrorResponseDTO("INVALID_REFRESH_TOKEN",
                                "Refresh token không hợp lệ hoặc đã hết hạn."));
            }

            String email = jwtService.extractEmail(refreshToken);
            String newAccessToken = jwtService.generateAccessToken(email);

            ResponseCookie newAccessCookie = ResponseCookie.from("AUTH_TOKEN", newAccessToken)
                    .httpOnly(true).secure(false).path("/").maxAge(15 * 60).sameSite("Lax").build();

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                    .body(new AuthResponseDTO("Access token mới đã được cấp.", "TOKEN_REFRESHED", "local",
                            newAccessToken, email, null));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponseDTO("REFRESH_FAILED", "Không thể làm mới token: " + e.getMessage()));
        }
    }

    private boolean isStrongPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }

    private void sendOtp(String email, String action) {
        String otp = otpService.generateOtp();
        otpService.storeOtp(email, otp);
        mailService.sendOtpMail(email, otp);
        log.info("[AuthController] OTP sent for {} to {}", action, email);
    }

    private ResponseEntity<?> issueTokensAndRedirect(String email, String provider, String message) {
        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken(email);

        ResponseCookie accessCookie = ResponseCookie.from("AUTH_TOKEN", accessToken)
                .httpOnly(true).secure(false).path("/").maxAge(15 * 60).sameSite("Lax").build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true).secure(false).path("/").maxAge(7 * 24 * 60 * 60).sameSite("Lax").build();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new AuthResponseDTO(message, "SUCCESS", provider, accessToken, email, "user/view/dashboard"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Xóa token cookie
        ResponseCookie clearAccess = ResponseCookie.from("AUTH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        ResponseCookie clearRefresh = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearAccess.toString());
        response.addHeader("Set-Cookie", clearRefresh.toString());

        // Xóa session và context bảo mật
        request.getSession().invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        try {
            log.info("🔎 Checking current user authentication: {}", auth != null ? auth.getName() : "null");

            UserDTO currentUser = authService.getCurrentUser(auth);
            if (currentUser == null) {
                log.warn("⚠️ No authenticated user found.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No authenticated user"));
            }

            // ✅ Lấy roles riêng, không chạm UserDTO
            Optional<User> userOpt = userRepository.findByEmailFetchRoles(currentUser.getEmail());
            List<String> roles = userOpt.isPresent()
                    ? userOpt.get().getRoles().stream().map(Role::getName).toList()
                    : List.of("ROLE_MEMBER");

            // ✅ Gói vào 1 JSON object
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user", currentUser);
            response.put("roles", roles);

            log.info("✅ Current user fetched successfully: {} with roles {}", currentUser.getEmail(), roles);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("⚠️ Unauthorized access attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid authentication state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("💥 Unexpected error in /api/auth/me: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal Server Error", "details", e.getMessage()));
        }
    }

}
