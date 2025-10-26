package com.devcollab.service.feature;

import com.devcollab.domain.Message;
import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;

import java.util.List;

public interface MessageService {
    List<MessageResponseDTO> getMessagesByProjectId(Long projectId);
    Message sendMessage(String senderUsername, MessageRequestDTO dto);
}