package com.devcollab.scheduler;

import com.devcollab.domain.Project;
import com.devcollab.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InviteLinkScheduler {

    private final ProjectRepository projectRepository;

    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ
    public void autoRegenExpiredInviteLinks() {
        List<Project> projects = projectRepository.findAll();

        for (Project p : projects) {
            boolean expired = p.getInviteExpiredAt() != null && p.getInviteExpiredAt().isBefore(LocalDateTime.now());
            boolean limitReached = p.getInviteUsageCount() >= p.getInviteMaxUses();

            if (p.isAllowLinkJoin() && p.isInviteAutoRegen() && (expired || limitReached)) {
                String newCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                p.setInviteLink(newCode);
                p.setInviteCreatedAt(LocalDateTime.now());
                p.setInviteExpiredAt(LocalDateTime.now().plusDays(7));
                p.setInviteUsageCount(0);
                p.setUpdatedAt(LocalDateTime.now());

                projectRepository.save(p);
                log.info("♻️ Auto-regenerated invite link for project '{}' -> {}", p.getName(), newCode);
            }
        }
    }
}
