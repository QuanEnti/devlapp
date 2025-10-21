package com.devcollab.controller.rest.userProfile;


import com.devcollab.dto.UserProfileDto.AvatarUploadResponseDto;
import com.devcollab.dto.UserProfileDto.UserPatchDto;
import com.devcollab.dto.UserProfileDto.UserProfileDto;
import com.devcollab.service.userprofileService.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileApi {

    private final UserProfileService userProfileService;

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileDto> getMyProfile(
            @RequestAttribute("userId") Long myUserId
    ) {
        return ResponseEntity.ok(userProfileService.getMyProfile(myUserId));
    }

    // 👀 2. Xem profile người khác
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getOtherUserProfile(id));
    }

    //3. Cập nhật profile cá nhân
    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @RequestAttribute("userId") Long myUserId,
            @RequestBody UserPatchDto patchDto) { // <-- Nhận DTO từ JSON body

        try {
            UserProfileDto updatedProfile = userProfileService.updateMyProfile(myUserId, patchDto);
            return ResponseEntity.ok(updatedProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // VD: User không tồn tại
        }
    }

    //4. Cập nhật avatar
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyAvatar(
            @RequestAttribute("userId") Long myUserId,
            @RequestParam("avatar") MultipartFile avatarFile) { // <-- Nhận file

        try {
            String newAvatarUrl = userProfileService.updateMyAvatar(myUserId, avatarFile);

            // Trả về DTO chứa URL mới
            return ResponseEntity.ok(new AvatarUploadResponseDto(newAvatarUrl));

        } catch (IOException e) {
            // Lỗi upload file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        } catch (RuntimeException e) {
            // Lỗi nghiệp vụ (VD: User không tìm thấy, file rỗng)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}
