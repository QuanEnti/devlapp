package com.devcollab.service.userprofileService;

import com.devcollab.domain.profileEnity.User;
import com.devcollab.dto.UserProfileDto.*;
import com.devcollab.repository.userprofileRepo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor // <-- THÊM VÀO: Hỗ trợ constructor injection
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final CloudinaryService cloudinaryService;

    // 🧍‍♂️ 1. Xem profile cá nhân (có project, task, collaborator)
    public UserProfileDto getMyProfile(Long myUserId) {
        User user = userProfileRepository.findByUserId(myUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ProjectSummaryDto> projects = user.getProjectMemberships().stream()
                .map(pm -> new ProjectSummaryDto(
                        pm.getProject().getProjectId(),
                        pm.getProject().getProjectName() // <-- Giả định bạn đã sửa Entity Project từ 'projectName' -> 'name'
                ))
                .distinct() // Giờ 'distinct' hoạt động đúng trên object
                .toList();

        // THAY ĐỔI: Logic mapping sang DTO con (TaskSummaryDto)
        List<TaskSumaryDto> tasks = user.getAssignedTasks().stream()
                .map(t -> new TaskSumaryDto(
                        t.getTaskId(),
                        t.getTitle()
                ))
                .toList();

        List<CollaboratorDto> collaborators = userProfileRepository
                .findCollaboratorsByUserId(myUserId)
                .stream()
                .map(CollaboratorDto::fromEntity)
                .toList();

        return new UserProfileDto(
                user.getAvatarUrl(),
                user.getName(),
                user.getBio(),
                user.getSkills(),
                user.getEmail(),
                projects,
                tasks,
                collaborators
        );
    }

    // 👀 2. Xem profile người khác (chỉ info + collaborators)
    public UserProfileDto getOtherUserProfile(Long targetUserId) {
        User user = userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CollaboratorDto> collaborators = userProfileRepository
                .findCollaboratorsByUserId(targetUserId)
                .stream()
                .map(CollaboratorDto::fromEntity)
                .toList();

        return new UserProfileDto(
                user.getAvatarUrl(),
                user.getName(),
                user.getBio(),
                user.getSkills(),
                null,          // không hiển thị email người khác
                null,          // không hiển thị project
                null,          // không hiển thị task
                collaborators  // vẫn hiển thị người làm việc cùng
        );
    }

    //3. Cập nhật thông tin profile cá nhân
    @Transactional // Ghi đè readOnly = true ở trên
    public UserProfileDto updateMyProfile(Long myUserId, UserPatchDto patchDto) {

        User user = userProfileRepository.findById(myUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + myUserId));

        // Kiểm tra từng trường trong DTO để hỗ trợ PATCH (cập nhật từng phần)
        if (patchDto.getName() != null) {
            user.setName(patchDto.getName());
        }
        if (patchDto.getBio() != null) {
            user.setBio(patchDto.getBio());
        }
        if (patchDto.getSkills() != null) {
            user.setSkills(patchDto.getSkills());
        }
        if (patchDto.getAvatarUrl() != null) {
            // Cho phép client set URL thủ công (ví dụ, sau khi upload)
            user.setAvatarUrl(patchDto.getAvatarUrl());
        }

        // Lưu các thay đổi vào DB
        userProfileRepository.save(user);

        // Trả về profile MỚI NHẤT (bao gồm cả tasks, projects...)
        return getMyProfile(myUserId);
    }

    // 4. Upload ảnh đại diện lên Cloudinary và trả về URL
    @Transactional // Ghi đè readOnly = true
    public String updateMyAvatar(Long myUserId, MultipartFile avatarFile) throws IOException {

        User user = userProfileRepository.findById(myUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + myUserId));

        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new RuntimeException("Avatar file is empty or missing");
        }

        // 1. Upload file lên Cloudinary
        String newAvatarUrl = cloudinaryService.uploadFile(avatarFile, "devcollab/avatars");

        // 2. Cập nhật URL mới cho User
        user.setAvatarUrl(newAvatarUrl);

        // 3. Lưu vào DB
        userProfileRepository.save(user);

        // 4. Trả về URL mới
        return newAvatarUrl;
    }
}
