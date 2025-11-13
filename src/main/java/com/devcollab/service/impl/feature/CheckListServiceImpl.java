package com.devcollab.service.impl.feature;

import com.devcollab.config.SpringContext;
import com.devcollab.domain.CheckList;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.CheckListDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.CheckListRepository;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.service.feature.CheckListService;
import com.devcollab.service.system.ProjectAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class CheckListServiceImpl implements CheckListService {

    private final CheckListRepository checkListRepo;
    private final TaskRepository taskRepo;
    private final TaskFollowerRepository followerRepo;

    @Override
    @Transactional(readOnly = true)
    public List<CheckListDTO> getByTask(Long taskId) {
        return checkListRepo.findByTask_TaskIdOrderByOrderIndex(taskId).stream().map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CheckListDTO addItem(Long taskId, String item, User actor) {

        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy task"));

        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();
        String email = actor.getEmail();
        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isFollower = followerRepo.existsByTask_TaskIdAndUser_UserId(taskId, actorId);
        if (!isPm && !isFollower) {
            throw new AccessDeniedException(
                    " Bạn phải là PM hoặc được giao vào task mới có thể thêm checklist.");
        }

        CheckList newItem = new CheckList();
        newItem.setTask(task);
        newItem.setItem(item);
        newItem.setCreatedBy(actor);
        newItem.setOrderIndex(checkListRepo.findByTask_TaskIdOrderByOrderIndex(taskId).size());

        return toDto(checkListRepo.save(newItem));
    }

    @Override
    @Transactional
    public CheckListDTO toggleItem(Long id, boolean done, User actor) {

        CheckList item = checkListRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist item không tồn tại"));

        Task task = item.getTask();
        Long projectId = task.getProject().getProjectId();
        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        try {
            authz.ensurePmOfProject(email, projectId);
        } catch (Exception e) {
            throw new AccessDeniedException("Chỉ PM hoặc ADMIN mới được tick checklist");
        }

        item.setIsDone(done);
        return toDto(checkListRepo.save(item));
    }

    @Override
    @Transactional
    public void deleteItem(Long id, User actor) {

        CheckList item = checkListRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist item không tồn tại"));

        Task task = item.getTask();
        Long projectId = task.getProject().getProjectId();
        Long actorId = actor.getUserId();
        String email = actor.getEmail();

        ProjectAuthorizationService authz =
                SpringContext.getBean(ProjectAuthorizationService.class);

        boolean isPm = false;
        try {
            authz.ensurePmOfProject(email, projectId);
            isPm = true;
        } catch (Exception ignored) {
        }

        boolean isCreator =
                (item.getCreatedBy() != null && item.getCreatedBy().getUserId().equals(actorId));

        if (!isPm && !isCreator) {
            throw new AccessDeniedException(
                    "Chỉ PM/ADMIN hoặc người tạo checklist mới được xóa mục này.");
        }
        checkListRepo.delete(item);
    }

    private CheckListDTO toDto(CheckList entity) {
        if (entity == null)
            return null;
        User creator = entity.getCreatedBy();
        return new CheckListDTO(entity.getChecklistId(), entity.getItem(), entity.getIsDone(),
                entity.getOrderIndex(), creator != null ? creator.getUserId() : null,
                creator != null ? creator.getName() : null);
    }


}
