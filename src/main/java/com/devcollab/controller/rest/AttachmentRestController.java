package com.devcollab.controller.rest;

import com.devcollab.domain.Attachment;
import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;
import com.devcollab.dto.AttachmentMemberInfo;
import com.devcollab.service.feature.AttachmentService;
import com.devcollab.service.system.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks/{taskId}/attachments")
public class AttachmentRestController {

    private final AttachmentService attachmentService;
    private final AuthService authService;

    // 🧾 1️⃣ Lấy danh sách attachment (file + link)
    @GetMapping
    public ResponseEntity<List<AttachmentDTO>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getAttachmentDTOsByTask(taskId));
    }

    // 📤 2️⃣ Upload file thực
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDTO> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        User uploader = authService.getCurrentUserEntity(auth);
        Attachment saved = attachmentService.uploadAttachment(taskId, file, uploader);

        AttachmentDTO dto = new AttachmentDTO(
                saved.getAttachmentId(),
                saved.getFileName(),
                saved.getFileUrl(),
                saved.getMimeType(),
                saved.getFileSize(),
                saved.getUploadedAt(),
                new AttachmentMemberInfo(
                        uploader.getUserId(),
                        uploader.getName(),
                        uploader.getAvatarUrl()));

        log.info("📎 Uploaded attachment '{}' for task {} by {}", saved.getFileName(), taskId, uploader.getEmail());
        return ResponseEntity.ok(dto);
    }

    // 🔗 3️⃣ Gắn link ngoài (Figma, Drive, Docs,...)
    @PostMapping(value = "/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttachmentDTO> attachLink(
            @PathVariable Long taskId,
            @RequestBody AttachmentDTO dto,
            Authentication auth) {

        User uploader = authService.getCurrentUserEntity(auth);
        Attachment saved = attachmentService.attachLink(taskId, dto.getFileName(), dto.getFileUrl(), uploader);

        AttachmentDTO result = new AttachmentDTO(
                saved.getAttachmentId(),
                saved.getFileName(),
                saved.getFileUrl(),
                saved.getMimeType(),
                saved.getFileSize(),
                saved.getUploadedAt(),
                new AttachmentMemberInfo(
                        uploader.getUserId(),
                        uploader.getName(),
                        uploader.getAvatarUrl()));

        log.info("🔗 Attached external link '{}' to task {} by {}", saved.getFileUrl(), taskId, uploader.getEmail());
        return ResponseEntity.ok(result);
    }

    // 🗑️ 4️⃣ Xóa attachment
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(attachmentId);
        log.info("🗑️ Deleted attachment id={}", attachmentId);
        return ResponseEntity.noContent().build();
    }

   @GetMapping("/download/**")
public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
    try {
        // ✅ Lấy phần path thực sự (sau /download/)
        String fullPath = (String) request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        // Ví dụ: /api/tasks/5/attachments/download/1730182838291_Token_ngắn_hạn_(ví_dụ_15_phút)_+_re.txt
        String filename = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, fullPath);

        // ✅ Decode UTF-8 để tránh lỗi Unicode
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

        Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", "attachments")
                .resolve(decodedFilename)
                .normalize();

        if (!Files.exists(filePath)) {
            log.warn("⚠️ File not found: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";

        log.info("📥 Serving file: {}", decodedFilename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);

    } catch (Exception e) {
        log.error("❌ Error serving file: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().build();
    }
}

}
