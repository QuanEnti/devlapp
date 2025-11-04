package com.devcollab.dto;

import java.time.LocalDateTime;

public class AttachmentDTO {

    private Long attachmentId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private int fileSize;
    private LocalDateTime uploadedAt;
    private AttachmentMemberInfo uploadedBy;

    public AttachmentDTO(Long attachmentId, String fileName, String fileUrl,
            String mimeType, int fileSize, LocalDateTime uploadedAt,
            AttachmentMemberInfo uploadedBy) {
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public AttachmentMemberInfo getUploadedBy() {
        return uploadedBy;
    }
}
