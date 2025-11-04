package com.devcollab.service.feature;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.devcollab.domain.Attachment;
import com.devcollab.domain.User;
import com.devcollab.dto.AttachmentDTO;

import java.io.IOException;

public interface AttachmentService {
    List<Attachment> getAttachmentsByTask(Long taskId);

    Attachment uploadAttachment(Long taskId, MultipartFile file, User uploader) throws IOException;

    void deleteAttachment(Long attachmentId);
    
    Attachment attachLink(Long taskId, String name, String url, User uploader);
    List<AttachmentDTO> getAttachmentDTOsByTask(Long taskId);

    List<AttachmentDTO> getRecentLinksByUser(Long userId);
}   
