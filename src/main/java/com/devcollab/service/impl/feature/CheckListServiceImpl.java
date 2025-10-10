package com.devcollab.service.impl.feature;

import com.devcollab.domain.CheckList;
import com.devcollab.domain.Task;
import com.devcollab.exception.BadRequestException;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.CheckListRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.CheckListService;
import com.devcollab.service.system.ActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckListServiceImpl implements CheckListService {

    private final CheckListRepository checkListRepository;
    private final TaskRepository taskRepository;
    private final ActivityService activityService;

    @Override
    public List<CheckList> getChecklistByTask(Long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        List<CheckList> items = checkListRepository.findByTask_TaskId(taskId);
        items.sort(Comparator.comparing(CheckList::getOrderIndex));
        return items;
    }

    @Override
    public CheckList createChecklistItem(Long taskId, String itemText) {
        if (itemText == null || itemText.isBlank()) {
            throw new BadRequestException("Nội dung checklist không được để trống");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        int nextIndex = checkListRepository.findByTask_TaskId(taskId).size() + 1;

        CheckList item = new CheckList();
        item.setTask(task);
        item.setItem(itemText);
        item.setIsDone(false);
        item.setOrderIndex(nextIndex);

        CheckList saved = checkListRepository.save(item);
        activityService.log("CHECKLIST", saved.getChecklistId(), "CREATE", saved.getItem());
        return saved;
    }

    @Override
    public CheckList toggleChecklistItem(Long checklistId, boolean isDone) {
        CheckList item = checkListRepository.findById(checklistId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mục checklist"));

        item.setIsDone(isDone);
        CheckList saved = checkListRepository.save(item);

        activityService.log("CHECKLIST", checklistId, isDone ? "CHECK" : "UNCHECK", saved.getItem());
        return saved;
    }

    @Override
    public CheckList updateChecklistItem(Long checklistId, String newText) {
        if (newText == null || newText.isBlank()) {
            throw new BadRequestException("Nội dung mới không được để trống");
        }

        CheckList item = checkListRepository.findById(checklistId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mục checklist"));

        item.setItem(newText);
        CheckList saved = checkListRepository.save(item);

        activityService.log("CHECKLIST", checklistId, "UPDATE", saved.getItem());
        return saved;
    }

    @Override
    public void deleteChecklistItem(Long checklistId) {
        if (!checkListRepository.existsById(checklistId)) {
            throw new NotFoundException("Mục checklist không tồn tại");
        }

        checkListRepository.deleteById(checklistId);
        activityService.log("CHECKLIST", checklistId, "DELETE", "Hard delete");
    }
}
