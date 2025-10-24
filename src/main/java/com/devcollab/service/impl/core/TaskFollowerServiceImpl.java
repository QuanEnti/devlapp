package com.devcollab.service.impl.core;

import com.devcollab.domain.Task;
import com.devcollab.domain.TaskFollower;
import com.devcollab.domain.User;
import com.devcollab.dto.TaskFollowerDTO;
import com.devcollab.exception.NotFoundException;
import com.devcollab.repository.TaskFollowerRepository;
import com.devcollab.repository.TaskRepository;
import com.devcollab.repository.UserRepository;
import com.devcollab.service.core.TaskFollowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskFollowerServiceImpl implements TaskFollowerService {

    private final TaskFollowerRepository followerRepo;
    private final TaskRepository taskRepo;
    private final UserRepository userRepo;

    @Override
    public List<TaskFollowerDTO> getFollowersByTask(Long taskId) {
        return followerRepo.findByTask_TaskId(taskId)
                .stream()
                .map(TaskFollowerDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public boolean assignMember(Long taskId, Long userId) {
        // 🔹 Kiểm tra đã tồn tại chưa
        if (followerRepo.existsByTaskAndUser(taskId, userId)) {
            System.out.println("⚠️ Member " + userId + " đã được gán vào task " + taskId);
            return false;
        }

        // 🔹 Kiểm tra task và user có tồn tại
        Task task = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // 🔹 Thêm vào bảng TaskFollower
        followerRepo.save(new TaskFollower(task, user));
        System.out.println("✅ Gán member " + userId + " vào task " + taskId + " thành công");
        return true;
    }

    @Override
    @Transactional
    public boolean unassignMember(Long taskId, Long userId) {
        // 🔹 Kiểm tra có tồn tại trước khi xóa
        boolean exists = followerRepo.existsByTaskAndUser(taskId, userId);
        if (!exists) {
            System.out.println("⚠️ Member " + userId + " chưa được gán trong task " + taskId);
            return false;
        }

        // 🔹 Xóa khỏi bảng TaskFollower
        followerRepo.deleteByTaskAndUser(taskId, userId);
        System.out.println("🗑️ Đã bỏ gán member " + userId + " khỏi task " + taskId);
        return true;
    }
}
