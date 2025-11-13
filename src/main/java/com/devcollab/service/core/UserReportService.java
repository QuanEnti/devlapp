package com.devcollab.service.core;

import com.devcollab.domain.User;
import com.devcollab.domain.UserReport;
import com.devcollab.dto.UserReportDto;
import com.devcollab.dto.request.ReportRequestDTO;


import java.util.List;
import java.util.Map;

public interface UserReportService {

    /** Lấy danh sách report cho admin (có phân trang) */
    Map<String, Object> getAllReports(int page, int size);

    /** Cập nhật trạng thái của report */
    UserReport updateReport(Long id, Map<String, String> body, User admin);

    /** Gửi cảnh báo (warning) cho user */
    void warnUser(Long id, Map<String, String> body);

    /** Ban user được report */
    void banUser(Long id,User admin);

    void createUserReport(ReportRequestDTO dto, String reporterEmail);

    UserReportDto getReportById(Long id);

    List<UserReportDto> getReportsByUser(String email);
}

