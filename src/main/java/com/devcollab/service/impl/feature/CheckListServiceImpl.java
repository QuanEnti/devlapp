package com.devcollab.service.impl.feature;

import com.devcollab.domain.CheckList;
import com.devcollab.domain.Task;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.CheckListRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.CheckListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class CheckListServiceImpl implements CheckListService {

    private final CheckListRepository checkListRepo;
    private final TaskRepository taskRepo;

    @Transactional(readOnly = true)
    public List<CheckList> getByTask(Long taskId) {
        return checkListRepo.findByTask_TaskIdOrderByOrderIndex(taskId);
    }

    @Transactional
    public CheckList addItem(Long taskId, String item) {
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));
        CheckList newItem = new CheckList();
        newItem.setTask(task);
        newItem.setItem(item);
        newItem.setOrderIndex(checkListRepo.findByTask_TaskIdOrderByOrderIndex(taskId).size());
        return checkListRepo.save(newItem);
    }

    @Transactional
    public CheckList toggleItem(Long id, boolean done) {
        CheckList item = checkListRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy checklist item"));
        item.setIsDone(done);
        return checkListRepo.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        if (!checkListRepo.existsById(id)) {
            throw new NotFoundException("Checklist item không tồn tại");
        }
        checkListRepo.deleteById(id);
    }
}
