package com.devcollab.scheduler;

import com.devcollab.domain.Notification;
import com.devcollab.repository.NotificationRepository;
import com.devcollab.service.system.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DigestScheduler {

    private final NotificationRepository notificationRepository;
    private final MailService mailService;

    @Scheduled(cron = "0 0 */2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendDigestEmails() {
        log.info("⏰ [DigestScheduler] Bắt đầu chạy gửi email tổng hợp (MEDIUM priority)...");

        List<Notification> pending = notificationRepository.findPendingMediumNotifications();
        if (pending == null || pending.isEmpty()) {
            log.info("💤 [DigestScheduler] Không có thông báo MEDIUM nào cần gửi mail.");
            return;
        }

        Map<String, List<Notification>> grouped = pending.stream()
                .filter(n -> n.getUser() != null && n.getUser().getEmail() != null)
                .collect(Collectors.groupingBy(n -> n.getUser().getEmail().trim()));

        int totalUsers = grouped.size();
        int totalNotis = pending.size();
        log.info("📦 [DigestScheduler] Chuẩn bị gửi digest cho {} người dùng ({} thông báo)...",
                totalUsers, totalNotis);

        grouped.forEach((email, notifs) -> {
            try {
                // 🔹 Tạo danh sách notification cho template
                List<Map<String, String>> digestList = notifs.stream()
                        .map(n -> Map.of(
                                "icon", mapIcon(n.getType()),
                                "message", Optional.ofNullable(n.getMessage()).orElse("(Không có nội dung)"),
                                "link", "https://devcollab.app" + (n.getLink() != null ? n.getLink() : "#")))
                        .toList();

                // 🔹 Gửi email digest HTML
                mailService.sendDigestMail(
                        email,
                        notifs.size() + " thông báo mới",
                        digestList,
                        "DevCollab Digest");

                // Đánh dấu đã gửi
                notifs.forEach(n -> n.setEmailed(true));

                log.info("✅ [DigestScheduler] Gửi digest thành công cho {} ({} mục)", email, notifs.size());
            } catch (Exception e) {
                log.error("❌ [DigestScheduler] Lỗi khi gửi digest cho {}: {}", email, e.getMessage());
            }
        });

        notificationRepository.saveAll(pending);
        log.info("📨 [DigestScheduler] Hoàn tất gửi digest cho {} người dùng.", totalUsers);
    }

    private String mapIcon(String type) {
        return switch (type) {
            case "TASK_COMMENT_MENTION" -> "📣";
            case "PROJECT_COMMENT_MENTION" -> "📢";
            case "TASK_DUE_SOON" -> "⏰";
            case "TASK_MEMBER_ADDED" -> "👥";
            case "TASK_MEMBER_REMOVED" -> "🚫";
            case "PROJECT_MEMBER_ROLE_UPDATED" -> "👤";
            case "PAYMENT_SUCCESS" -> "💰";
            default -> "📬";
        };
    }
}
