package com.devcollab.controller.rest;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;
import com.devcollab.service.feature.AttachmentService;
import com.devcollab.service.system.AuthService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attachments")
@Slf4j
public class AttachmentGlobalController {

    private final AttachmentService attachmentService;
    private final AuthService authService;

    @GetMapping("/recent-links")
    public ResponseEntity<List<AttachmentDTO>> getRecentLinks(Authentication auth) {
        User user = authService.getCurrentUserEntity(auth);
        List<AttachmentDTO> recentLinks = attachmentService.getRecentLinksByUser(user.getUserId());
        log.info("🕓 Fetched {} recent links for user {}", recentLinks.size(), user.getEmail());
        return ResponseEntity.ok(recentLinks);
    }
}
