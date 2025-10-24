package com.devcollab.dto.request;
public record TaskQuickCreateReq(
        String title,
        Long columnId,
        Long projectId) {
}
