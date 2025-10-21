package com.devcollab.dto.taskDto;


import com.devcollab.domain.Attachment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {
    private Long attachmentId;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private long fileSize;
    private LocalDateTime uploadedAt;
    private UserSummaryDto uploadedBy; // DTO User tóm tắt (đã tạo ở lần trước)

    public static AttachmentDto fromEntity(Attachment attachment) {
        return new AttachmentDto(
                attachment.getAttachmentId(),
                attachment.getFileUrl(),
                attachment.getFileName(),
                attachment.getMimeType(),
                attachment.getFileSize(),
                attachment.getUploadedAt(),
                UserSummaryDto.fromEntity(attachment.getUploadedBy())
        );
    }

}
