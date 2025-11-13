package com.devcollab.repository;

import com.devcollab.domain.CheckList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CheckListRepository extends JpaRepository<CheckList, Long> {
    List<CheckList> findByTask_TaskIdOrderByOrderIndex(Long taskId);
}
