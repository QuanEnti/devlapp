package com.devcollab.repository;

import com.devcollab.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByProject_ProjectIdOrderByCreatedAtAsc(Long projectId);
}
