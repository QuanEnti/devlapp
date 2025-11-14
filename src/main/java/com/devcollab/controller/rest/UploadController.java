package com.devcollab.controller.rest;

import com.devcollab.service.system.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadFile(file);
        return ResponseEntity.ok(imageUrl);
    }
    @PostMapping("/project-cover")
    public ResponseEntity<?> uploadProjectCover(@RequestParam("file") MultipartFile file) {
        String imageUrl = cloudinaryService.uploadFile(file);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "url", imageUrl
        ));
    }
}
