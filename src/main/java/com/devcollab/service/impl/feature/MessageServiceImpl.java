package com.devcollab.service.impl.feature;

import com.devcollab.domain.Message;
import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import com.devcollab.dto.request.MessageRequestDTO;
import com.devcollab.dto.response.MessageResponseDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.MessageRepository;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.feature.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public List<MessageResponseDTO> getMessagesByProjectId(Long projectId) {
        List<Message> messages = messageRepository.findByProject_ProjectIdOrderByCreatedAtAsc(projectId);

        return messages.stream().map(msg -> new MessageResponseDTO(
                msg.getMessageId(),
                msg.getSender().getName(),
                msg.getSender().getEmail(),
                msg.getContent(),
                msg.getCreatedAt()
        )).collect(Collectors.toList());
    }

    @Override
    public Message sendMessage(String senderUsername, MessageRequestDTO dto) {
        User sender = userRepository.findByEmail(senderUsername)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new NotFoundException("Dự án không tồn tại"));

        Message message = new Message();
        message.setSender(sender);
        message.setProject(project);
        message.setContent(dto.getContent());

        return messageRepository.save(message);
    }
}