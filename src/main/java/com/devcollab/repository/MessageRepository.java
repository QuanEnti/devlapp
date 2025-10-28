package com.devcollab.repository;

import com.devcollab.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import com.devcollab.domain.User;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByProject_ProjectIdOrderByCreatedAtAsc(Long projectId);
    List<Message> findAllMessageBySender(User sender);
}
