package com.devcollab.service.taskService;


import com.devcollab.domain.Attachment;
import com.devcollab.domain.Comment;
import com.devcollab.domain.Task;
import com.devcollab.domain.User;
import com.devcollab.dto.taskDto.*;
import com.devcollab.repository.taskRepository.*;
import com.devcollab.service.userprofileService.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository; // <-- Dùng để check bảo mật
    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public TaskDetailDto getTaskDetails(Long taskId, Long currentUserId) throws AccessDeniedException {

        // B1: Lấy Task bằng query đã tối ưu (EntityGraph)
        Task task = taskRepository.findDetailedById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

        // B2: KIỂM TRA BẢO MẬT (Rất quan trọng)
        // User chỉ được xem task nếu họ là thành viên của project đó
        Long projectId = task.getProject().getProjectId();

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId);

        if (!isMember) {
            // Nếu không phải thành viên, ném lỗi 403 Forbidden
            throw new AccessDeniedException("User is not a member of the project for this task");
        }

        // B3: Nếu qua được, map Entity sang DTO và trả về
        return TaskDetailDto.fromEntity(task);
    }

    @Transactional // Bật chế độ ghi
    public TaskDetailDto updateTaskStatus(Long taskId, Long currentUserId, TaskStatusUpdateDto statusUpdate) {

        // B1: Kiểm tra quyền và lấy Task
        Task task = checkProjectMembershipAndGetTask(taskId, currentUserId);

        // B2: Cập nhật status
        String newStatus = statusUpdate.getStatus();
        task.setStatus(newStatus);

        // B3: Logic nghiệp vụ: Nếu là "DONE" (Completed), đóng task.
        if ("DONE".equals(newStatus)) {
            task.setClosedAt(LocalDateTime.now());
        } else {
            // Nếu chuyển từ DONE về trạng thái khác, mở lại task
            task.setClosedAt(null);
        }

        taskRepository.save(task);

        // B4: Trả về DTO chi tiết đầy đủ của task (đã cập nhật)
        // Gọi lại hàm gốc để lấy @EntityGraph
        return getTaskDetails(taskId, currentUserId);
    }

    @Transactional // Bật chế độ ghi
    public AttachmentDto addAttachment(Long taskId, Long currentUserId, MultipartFile file) throws IOException {

        // B1: Kiểm tra quyền và lấy tham chiếu
        Task task = checkProjectMembershipAndGetTask(taskId, currentUserId);
        User user = userRepository.getReferenceById(currentUserId);

        // B2: Upload file lên Cloud
        String fileUrl = cloudinaryService.uploadFile(file, "devcollab/attachments/" + taskId);

        // B3: Tạo và lưu Entity Attachment
        Attachment attachment = new Attachment();
        attachment.setFileUrl(fileUrl);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setMimeType(file.getContentType());
        attachment.setFileSize((int) file.getSize());
        attachment.setTask(task);
        attachment.setUploadedBy(user);

        Attachment savedAttachment = attachmentRepository.save(attachment);

        // B4: Trả về DTO của file đính kèm mới (Load lại để lấy 'uploadedBy')
        Attachment result = attachmentRepository.findById(savedAttachment.getAttachmentId()).get();
        return AttachmentDto.fromEntity(result);
    }

    @Transactional // Ghi đè readOnly=true, bật chế độ ghi
    public CommentDto addComment(Long taskId, Long currentUserId, CommentRequestDto commentRequest) {

        // B1: Kiểm tra quyền và lấy tham chiếu
        Task task = checkProjectMembershipAndGetTask(taskId, currentUserId);
        User user = userRepository.getReferenceById(currentUserId);

        // B2: Tạo và lưu Comment
        Comment comment = new Comment(commentRequest.getContent(), task, user);
        Comment savedComment = commentRepository.save(comment);

        // B3: Trả về DTO của comment mới (Load lại để lấy 'user' đã eager load)
        Comment result = commentRepository.findById(savedComment.getCommentId()).get();
        return CommentDto.fromEntity(result);
    }



    /**
     * (HÀM MỚI) Enum để định nghĩa các loại filter
     */
    public enum TaskFilter {
        ALL, // Tất cả task
        ME   // Chỉ task của tôi
    }

    public List<TaskListItemDto> getTasksByProject(Long projectId, Long currentUserId, TaskFilter filter) {

        // B1: KIỂM TRA BẢO MẬT (quan trọng!)
        // User phải là thành viên của project mới được xem task
        checkProjectMembership(projectId, currentUserId);

        // B2: Lấy dữ liệu Task Entities dựa trên filter
        List<Task> tasks;
        if (filter == TaskFilter.ME) {
            // Lọc "Chỉ task của tôi"
            tasks = taskRepository.findByProjectIdAndAssigneeId(projectId, currentUserId);
        } else {
            // Mặc định là "Tất cả task"
            tasks = taskRepository.findAllByProjectId(projectId);
        }

        // B3: Chuyển đổi List<Task> sang List<TaskListItemDto>
        return tasks.stream()
                .map(TaskListItemDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ... (Giữ nguyên các hàm getTaskDetails, addComment, v.v...)


    /**
     * HÀM HELPER (Private)
     * (Đã viết ở lần trước, dùng để kiểm tra bảo mật)
     */
    private void checkProjectMembership(Long projectId, Long userId) {
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        if (!isMember) {
            throw new AccessDeniedException("User is not a member of this project");
        }
    }

    /**
     * (Helper 2)
     * Hàm tiện lợi: Vừa check quyền, vừa trả về Task
     * Dùng cho các hàm C/U/D (Comment, Attachment, Status Update)
     */
    private Task checkProjectMembershipAndGetTask(Long taskId, Long userId) {
        // Lấy task (không cần @EntityGraph vì chỉ cần ID project)
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        // Dùng helper 1 để check quyền
        checkProjectMembership(task.getProject().getProjectId(), userId);

        return task;
    }


}
