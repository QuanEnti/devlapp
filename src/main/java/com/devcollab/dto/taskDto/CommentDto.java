package com.devcollab.dto.taskDto;

import com.devcollab.domain.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long commentId;
    private String content;
    private LocalDateTime createdAt;
    private UserSummaryDto user; // <-- Lồng DTO user vào

    public static CommentDto fromEntity(Comment comment) {
        return new CommentDto(
                comment.getCommentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                UserSummaryDto.fromEntity(comment.getUser())
        );
    }
}
