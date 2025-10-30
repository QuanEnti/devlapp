package com.devcollab.dto.userTaskDto;

/**
 * DTO "siêu nhẹ" chỉ dùng để đổ (populate)
 * dropdown lọc project trên frontend.
 */
public record ProjectFilterDTO(
    Long projectId,
    String name
) {}
