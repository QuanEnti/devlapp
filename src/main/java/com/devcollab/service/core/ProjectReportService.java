package com.devcollab.service.core;

import java.util.Map;

public interface ProjectReportService {

    /** Lấy tất cả các report của project (phân trang) */
    Map<String, Object> getAllReports(int page, int size);

    /** Gửi cảnh báo (warning) cho chủ dự án */
    void warnOwner(Long reportId, Map<String, String> body);

    /** Xóa / ban dự án */
    void removeProject(Long reportId);
}