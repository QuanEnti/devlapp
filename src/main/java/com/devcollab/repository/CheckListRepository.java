package com.devcollab.repository;

import com.devcollab.domain.CheckList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CheckListRepository extends JpaRepository<CheckList, Long> {
    List<CheckList> findByTask_TaskId(Long taskId);
}
