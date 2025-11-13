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
import org.springframework.security.access.AccessDeniedException;
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
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks/{taskId}/attachments")
public class AttachmentRestController {

    private final AttachmentService attachmentService;
    private final AuthService authService;

    // 🧾 1️⃣ Lấy danh sách attachment
    @GetMapping
    public ResponseEntity<List<AttachmentDTO>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getAttachmentDTOsByTask(taskId));
    }


    // 📤 2️⃣ Upload file vào task
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentDTO> uploadAttachment(@PathVariable Long taskId,
            @RequestParam("file") MultipartFile file, Authentication auth) throws IOException {

        User uploader = authService.getCurrentUserEntity(auth);

        Attachment saved = attachmentService.uploadAttachment(taskId, file, uploader);

        AttachmentDTO dto = new AttachmentDTO(saved.getAttachmentId(), saved.getFileName(),
                saved.getFileUrl(), saved.getMimeType(), saved.getFileSize(), saved.getUploadedAt(),
                new AttachmentMemberInfo(uploader.getUserId(), uploader.getName(),
                        uploader.getAvatarUrl()));

        log.info("📎 Uploaded attachment '{}' for task {} by {}", saved.getFileName(), taskId,
                uploader.getEmail());

        return ResponseEntity.ok(dto);
    }


    // 🔗 3️⃣ Gắn link
    @PostMapping(value = "/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AttachmentDTO> attachLink(@PathVariable Long taskId,
            @RequestBody AttachmentDTO dto, Authentication auth) {

        User uploader = authService.getCurrentUserEntity(auth);

        Attachment saved =
                attachmentService.attachLink(taskId, dto.getFileName(), dto.getFileUrl(), uploader);

        AttachmentDTO result = new AttachmentDTO(saved.getAttachmentId(), saved.getFileName(),
                saved.getFileUrl(), saved.getMimeType(), saved.getFileSize(), saved.getUploadedAt(),
                new AttachmentMemberInfo(uploader.getUserId(), uploader.getName(),
                        uploader.getAvatarUrl()));

        log.info("🔗 Attached link '{}' for task {} by {}", saved.getFileUrl(), taskId,
                uploader.getEmail());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long taskId,
            @PathVariable Long attachmentId, Authentication auth) {

        User actor = authService.getCurrentUserEntity(auth);

        try {
            attachmentService.deleteAttachment(attachmentId, actor.getEmail());
            log.info("🗑️ Attachment {} deleted by {}", attachmentId, actor.getEmail());
            return ResponseEntity.noContent().build();

        } catch (AccessDeniedException ex) {

            return ResponseEntity.status(403)
                    .body(Map.of("message", "Bạn không có quyền xóa file/link này"));

        } catch (Exception ex) {

            log.error("❌ Error deleting attachment: {}", ex.getMessage(), ex);

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Lỗi server khi xóa file/link"));
        }
    }



    // 📥 5️⃣ Download file vật lý
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {

        try {
            String fullPath = (String) request
                    .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String bestPattern =
                    (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

            String filename = new AntPathMatcher().extractPathWithinPattern(bestPattern, fullPath);

            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", "attachments")
                    .resolve(decodedFilename).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null)
                contentType = "application/octet-stream";

            log.info("📥 Serving file: {}", decodedFilename);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Error serving file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
