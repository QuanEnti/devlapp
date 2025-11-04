package com.devcollab.service.impl.feature;

import com.devcollab.domain.Attachment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;
import com.devcollab.repository.AttachmentRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.AttachmentService;
import com.devcollab.service.system.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final ActivityService activityService; // 🟢 Thêm vào

    private final Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "attachments");

    @Override
    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsByTask(Long taskId) {
        return attachmentRepository.findActiveByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDTO> getAttachmentDTOsByTask(Long taskId) {
        return attachmentRepository.findActiveAttachmentDTOs(taskId);
    }

    @Override
    @Transactional
    public Attachment uploadAttachment(Long taskId, MultipartFile file, User uploader) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        if (uploader == null || uploader.getUserId() == null)
            throw new IllegalStateException("Uploader not found or not saved in database");

        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String safeFileName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = uploadDir.resolve(safeFileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        String fileUrl = "/api/tasks/" + taskId + "/attachments/download/" + safeFileName;

        // 🔍 Check file trùng theo (taskId + fileName)
        List<Attachment> existingFiles = attachmentRepository.findByTaskAndFileName(task, originalName);
        if (!existingFiles.isEmpty()) {
            // ✅ Nếu có, lấy version lớn nhất rồi tăng thêm 1
            int nextVersion = existingFiles.stream()
                    .mapToInt(a -> a.getVersion() != null ? a.getVersion() : 1)
                    .max()
                    .orElse(1) + 1;

            Attachment attachment = new Attachment();
            attachment.setTask(task);
            attachment.setFileName(originalName);
            attachment.setFileUrl(fileUrl);
            attachment.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            attachment.setFileSize((int) file.getSize());
            attachment.setUploadedBy(uploader);
            attachment.setUploadedAt(LocalDateTime.now());
            attachment.setVersion(nextVersion);

            Attachment saved = attachmentRepository.save(attachment);

            // 🟢 Log
            activityService.log(
                    "TASK",
                    taskId,
                    "ATTACH_FILE_VERSION",
                    "{\"fileName\":\"" + escapeJson(originalName) + "\",\"version\":" + nextVersion + "}",
                    uploader);

            log.info("📎 Uploaded new version {} of '{}' ({} bytes) for task {} by {}",
                    nextVersion, originalName, file.getSize(), taskId, uploader.getEmail());
            return saved;
        }

        // 🆕 Nếu file mới hoàn toàn → tạo version = 1
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(originalName);
        attachment.setFileUrl(fileUrl);
        attachment.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setFileSize((int) file.getSize());
        attachment.setUploadedBy(uploader);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setVersion(1);

        Attachment saved = attachmentRepository.save(attachment);

        // 🟢 Log Trello-style
        activityService.log(
                "TASK",
                taskId,
                "ATTACH_FILE",
                "{\"fileName\":\"" + escapeJson(originalName) + "\"}",
                uploader);

        log.info("📎 Uploaded new attachment '{}' ({} bytes) for task {} by {}",
                originalName, file.getSize(), taskId, uploader.getEmail());
        return saved;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        String url = att.getFileUrl();

        if (url != null && url.contains("/attachments/download/")) {
            try {
                String filename = url.substring(url.lastIndexOf("/") + 1);
                Path filePath = uploadDir.resolve(filename);
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("⚠️ Could not delete physical file: {}", e.getMessage());
            }
        }

        att.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(att);

        // 🟢 Log xóa file
        if (att.getUploadedBy() != null) {
            activityService.log(
                    "TASK",
                    att.getTask().getTaskId(),
                    "DELETE_ATTACHMENT",
                    "{\"fileName\":\"" + escapeJson(att.getFileName()) + "\"}",
                    att.getUploadedBy());
        }

        log.info("🗑️ Marked as deleted attachment id={}", attachmentId);
    }

    @Override
    @Transactional
    public Attachment attachLink(Long taskId, String name, String url, User uploader) {
        if (uploader == null || uploader.getUserId() == null)
            throw new IllegalStateException("Uploader not found or not saved in database");

        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL must not be empty");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 🔍 Kiểm tra link cũ
        Optional<Attachment> existingOpt = attachmentRepository.findByTaskAndFileUrl(task, url);
        if (existingOpt.isPresent()) {
            Attachment existing = existingOpt.get();

            // Nếu link đã bị xóa → revive
            if (existing.getDeletedAt() != null) {
                existing.setDeletedAt(null);
            }

            // ✅ Nếu nhập tên mới → cập nhật lại hiển thị
            if (name != null && !name.isBlank() && !name.equals(existing.getFileName())) {
                existing.setFileName(name.trim());
            }

            existing.setUploadedAt(LocalDateTime.now());
            existing.setUploadedBy(uploader);

            Attachment updated = attachmentRepository.save(existing);

            // Log update lại link
            activityService.log(
                    "TASK",
                    taskId,
                    "UPDATE_LINK",
                    "{\"link\":\"" + escapeJson(url) + "\",\"name\":\"" + escapeJson(updated.getFileName()) + "\"}",
                    uploader);

            return updated;
        }

        // 🆕 Nếu link chưa tồn tại → thêm mới
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(
                name != null && !name.isBlank() ? name.trim() : "Link " + LocalDateTime.now().toString());
        attachment.setFileUrl(url);
        attachment.setMimeType("link/url");
        attachment.setFileSize(0);
        attachment.setUploadedBy(uploader);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setLink(true);

        Attachment saved = attachmentRepository.save(attachment);

        // 🟢 Ghi log
        activityService.log(
                "TASK",
                taskId,
                "ATTACH_LINK",
                "{\"link\":\"" + escapeJson(url) + "\",\"name\":\"" + escapeJson(attachment.getFileName()) + "\"}",
                uploader);

        log.info("🔗 Attached new link '{}' to task {} by {}", name, taskId, uploader.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachmentDTO> getRecentLinksByUser(Long userId) {
        return attachmentRepository.findRecentLinksByUser(userId);
    }

    private String escapeJson(String text) {
        return text == null ? "" : text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
