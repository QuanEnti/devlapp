package com.devcollab.service.core;
import com.devcollab.dto.request.ScheduleRequestDTO;
import com.devcollab.dto.response.ScheduleResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface ProjectScheduleService {

    ScheduleResponseDTO createSchedule(Long projectId, ScheduleRequestDTO dto);
    List<ScheduleResponseDTO> getSchedules(Long projectId);
    ScheduleResponseDTO getScheduleById(Long scheduleId);
    ScheduleResponseDTO updateSchedule(Long scheduleId, ScheduleRequestDTO dto);
    void deleteSchedule(Long scheduleId);
    List<ScheduleResponseDTO> getSchedulesByDate(Long projectId, LocalDate date);
    List<LocalDate> getScheduledDates(Long projectId, int year, int month);
}