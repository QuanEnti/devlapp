package com.devcollab.service.system;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskDeadlineReminderJob {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final Map<String, Duration> REMINDER_STAGES = Map.of("24h", Duration.ofHours(24),
            "1h", Duration.ofHours(1), "5m", Duration.ofMinutes(5));

    @Scheduled(fixedRate = 30 * 1000)

    public void checkUpcomingDeadlines() {
        List<Task> tasks = taskRepository.findTasksDueBetween(LocalDateTime.now(),
                LocalDateTime.now().plusHours(24));

        LocalDateTime now = LocalDateTime.now();

        for (Task task : tasks) {
            if (task.getDeadline() == null)
                continue;

            for (Map.Entry<String, Duration> stageEntry : REMINDER_STAGES.entrySet()) {
                String stage = stageEntry.getKey();
                Duration duration = stageEntry.getValue();

                LocalDateTime targetTime = task.getDeadline().minus(duration);
                long diffMin = Duration.between(now, targetTime).toMinutes();

                log.info("⏱️ [Debug] Task '{}' stage={} deadline={} now={} target={} diff={} phút",
                        task.getTitle(), stage, task.getDeadline(), now, targetTime, diffMin);

                if (Math.abs(diffMin) > 60)
                    continue; // mở rộng 1h để test

                String redisKey = "task:reminder:" + task.getTaskId() + ":" + stage;

                boolean redisSent = Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
                boolean dbSentRecently =
                        task.getLastRemindAt() != null && task.getLastReminderStage() != null
                                && task.getLastReminderStage().equals(stage)
                                && Duration.between(task.getLastRemindAt(), now).toHours() < 24;

                if (redisSent || dbSentRecently)
                    continue;

                sendReminderToAll(task, stage);

                redisTemplate.opsForValue().set(redisKey, "sent", getTTLMinutes(stage),
                        TimeUnit.MINUTES);

                task.setLastRemindAt(LocalDateTime.now());
                task.setLastReminderStage(stage);
                taskRepository.save(task);

                log.info("🔔 [HybridReminder] Gửi nhắc '{}' cho task '{}' (ID={})", stage,
                        task.getTitle(), task.getTaskId());
            }
        }
    }

    // Gửi cho tất cả người liên quan: assignee + người tạo + follower
    private void sendReminderToAll(Task task, String stage) {
        String title = "⏰ Nhắc hạn công việc";
        String message = switch (stage) {
            case "24h" -> "Công việc \"" + task.getTitle() + "\" sẽ đến hạn trong 24 giờ!";
            case "1h" -> "Công việc \"" + task.getTitle() + "\" sẽ đến hạn sau 1 giờ!";
            case "5m" -> "Công việc \"" + task.getTitle() + "\" sắp đến hạn sau 5 phút!";
            default -> "Công việc \"" + task.getTitle() + "\" sắp đến hạn!";
        };

        String link = "/view/pm/project/board?projectId=" + task.getProject().getProjectId()
                + "&taskId=" + task.getTaskId();

        List<User> receivers = new ArrayList<>();

        if (task.getAssignee() != null)
            receivers.add(task.getAssignee());
        if (task.getCreatedBy() != null)
            receivers.add(task.getCreatedBy());
        if (task.getFollowers() != null) {
            task.getFollowers().forEach(f -> {
                if (f.getUser() != null)
                    receivers.add(f.getUser());
            });
        }

        // Lọc trùng userId
        List<User> uniqueReceivers =
                receivers.stream().filter(Objects::nonNull).filter(u -> u.getUserId() != null)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(User::getUserId, u -> u, (a, b) -> a),
                                m -> new ArrayList<>(m.values())));

        if (uniqueReceivers.isEmpty()) {
            log.debug("ℹ️ Không có người nhận nhắc hạn cho task '{}'", task.getTitle());
            return;
        }

        for (User receiver : uniqueReceivers) {
            notificationService.createNotification(receiver, "TASK_DUE_SOON", task.getTaskId(),
                    title, message, link, null);
        }

        log.info("📨 Đã gửi nhắc hạn '{}' tới {} người cho task '{}'", stage,
                uniqueReceivers.size(), task.getTitle());
    }

    private long getTTLMinutes(String stage) {
        return switch (stage) {
            case "24h" -> 60 * 30; // 30h
            case "1h" -> 240; // 4h
            case "5m" -> 30; // 30 phút
            default -> 60;
        };
    }
}
