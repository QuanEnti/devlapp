package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.Attachment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;
import com.devcollab.repository.AttachmentRepository;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.feature.AttachmentService;
import com.devcollab.service.system.ActivityService;
import com.devcollab.service.system.ProjectAuthorizationService;
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
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final ActivityService activityService;
    private final ProjectAuthorizationService projectAuthService;
    private final UserRepository userRepository;
    private final TaskFollowerRepository followerRepo;

    private final Path uploadDir =
            Paths.get(System.getProperty("user.dir"), "uploads", "attachments");

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
    public Attachment uploadAttachment(Long taskId, MultipartFile file, User uploader)
            throws IOException {

        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        if (uploader == null || uploader.getUserId() == null)
            throw new IllegalStateException("Uploader not found or not saved in database");

        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Long projectId = task.getProject().getProjectId();
        Long userId = uploader.getUserId();
        String email = uploader.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPmOrAdmin = false;

        try {
            authz.ensurePmOfProject(email, projectId);
            isPmOrAdmin = true;
        } catch (Exception ignored) {
            isPmOrAdmin = false;
        }

        boolean isFollower = followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, userId);

        if (!isPmOrAdmin && !isFollower) {
            throw new AccessDeniedException(
                    "Bạn phải là PM/ADMIN hoặc thành viên được gán vào task mới có thể upload file.");
        }


        String originalName =
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";

        String safeFileName =
                System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        Path target = uploadDir.resolve(safeFileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/api/tasks/" + taskId + "/attachments/download/" + safeFileName;

        List<Attachment> existingFiles =
                attachmentRepository.findByTaskAndFileName(task, originalName);

        if (!existingFiles.isEmpty()) {
            int nextVersion = existingFiles.stream()
                    .mapToInt(a -> a.getVersion() != null ? a.getVersion() : 1).max().orElse(1) + 1;

            Attachment versioned = new Attachment();
            versioned.setTask(task);
            versioned.setFileName(originalName);
            versioned.setFileUrl(fileUrl);
            versioned.setMimeType(file.getContentType());
            versioned.setFileSize((int) file.getSize());
            versioned.setUploadedBy(uploader);
            versioned.setUploadedAt(LocalDateTime.now());
            versioned.setVersion(nextVersion);

            Attachment saved = attachmentRepository.save(versioned);

            activityService.log(
                    "TASK", taskId, "ATTACH_FILE_VERSION", "{\"fileName\":\""
                            + escapeJson(originalName) + "\",\"version\":" + nextVersion + "}",
                    uploader);

            log.info("📎 Uploaded version {} of '{}' for task {} by {}", nextVersion, originalName,
                    taskId, email);

            return saved;
        }

        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(originalName);
        attachment.setFileUrl(fileUrl);
        attachment.setMimeType(file.getContentType());
        attachment.setFileSize((int) file.getSize());
        attachment.setUploadedBy(uploader);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setVersion(1);

        Attachment saved = attachmentRepository.save(attachment);

        activityService.log("TASK", taskId, "ATTACH_FILE",
                "{\"fileName\":\"" + escapeJson(originalName) + "\"}", uploader);

        log.info("📎 Uploaded new file '{}' for task {} by {}", originalName, taskId, email);

        return saved;
    }


    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, String email) {

        Attachment att = attachmentRepository.findById(attachmentId).orElseThrow(
                () -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        Long projectId = att.getTask().getProject().getProjectId();

        Long currentUserId = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User không tồn tại")).getUserId();

        boolean isPmOrAdmin = false;
        try {
            projectAuthService.ensurePmOfProject(email, projectId);
            isPmOrAdmin = true;
        } catch (AccessDeniedException ex) {
            isPmOrAdmin = false;
        }

        boolean isUploader = att.getUploadedBy() != null
                && att.getUploadedBy().getUserId().equals(currentUserId);

        if (!isPmOrAdmin && !isUploader) {
            throw new AccessDeniedException("Bạn không có quyền xóa file này");
        }

        String url = att.getFileUrl();

        if (url != null && url.contains("/attachments/download/")) {
            try {
                String filename = url.substring(url.lastIndexOf("/") + 1);
                Files.deleteIfExists(uploadDir.resolve(filename));
            } catch (Exception e) {
                log.error("⚠️ Could not delete file: {}", e.getMessage());
            }
        }

        att.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(att);

        activityService.log("TASK", att.getTask().getTaskId(), "DELETE_ATTACHMENT",
                "{\"fileName\":\"" + escapeJson(att.getFileName()) + "\"}", att.getUploadedBy());

        log.info("Attachment {} deleted by {}", attachmentId, email);
    }


    @Override
    @Transactional
    public Attachment attachLink(Long taskId, String name, String url, User uploader) {

        if (uploader == null || uploader.getUserId() == null)
            throw new IllegalStateException("Uploader not found");

        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL must not be empty");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        // 🔐 CHECK QUYỀN UPLOAD LINK
        ensureCanUpload(task, uploader);

        // ========== KIỂM TRA LINK CŨ ==========
        Optional<Attachment> existingOpt = attachmentRepository.findByTaskAndFileUrl(task, url);

        if (existingOpt.isPresent()) {
            Attachment existing = existingOpt.get();

            // revive if deleted
            if (existing.getDeletedAt() != null)
                existing.setDeletedAt(null);

            // update name if provided
            if (name != null && !name.isBlank())
                existing.setFileName(name.trim());

            existing.setUploadedAt(LocalDateTime.now());
            existing.setUploadedBy(uploader);
            Attachment updated = attachmentRepository.save(existing);

            activityService.log(
                    "TASK", taskId, "UPDATE_LINK", "{\"link\":\"" + escapeJson(url)
                            + "\",\"name\":\"" + escapeJson(updated.getFileName()) + "\"}",
                    uploader);

            return updated;
        }

        // ========== TẠO LINK MỚI ==========
        Attachment attachment = new Attachment();
        attachment.setTask(task);
        attachment.setFileName(
                name != null && !name.isBlank() ? name.trim() : "Link " + LocalDateTime.now());
        attachment.setFileUrl(url);
        attachment.setMimeType("link/url");
        attachment.setFileSize(0);
        attachment.setUploadedBy(uploader);
        attachment.setUploadedAt(LocalDateTime.now());
        attachment.setLink(true);

        Attachment saved = attachmentRepository.save(attachment);

        activityService.log("TASK", taskId, "ATTACH_LINK", "{\"link\":\"" + escapeJson(url)
                + "\",\"name\":\"" + escapeJson(attachment.getFileName()) + "\"}", uploader);

        log.info("🔗 Attached link '{}' to task {} by {}", name, taskId, uploader.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachmentDTO> getRecentLinksByUser(Long userId) {
        return attachmentRepository.findRecentLinksByUser(userId);
    }

    private String escapeJson(String text) {
        return text == null ? ""
                : text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private void ensureCanUpload(Task task, User uploader) {

        Long projectId = task.getProject().getProjectId();
        Long userId = uploader.getUserId();
        String email = uploader.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPmOrAdmin = false;

        try {
            authz.ensurePmOfProject(email, projectId);
            isPmOrAdmin = true;
        } catch (Exception ignored) {
            isPmOrAdmin = false;
        }

        boolean isFollower =
                followerRepo.existsByTask_TaskIdAndUser_UserId(task.getTaskId(), userId);

        if (!isPmOrAdmin && !isFollower) {
            throw new AccessDeniedException(
                    "Bạn phải được giao vào công việc hoặc là PM/ADMIN mới có quyền upload file/link.");
        }
    }

}
