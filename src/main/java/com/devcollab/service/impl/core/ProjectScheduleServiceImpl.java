package com.devcollab.service.impl.core;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectSchedule;
import com.devcollab.domain.User;
import com.devcollab.dto.request.ScheduleRequestDTO;
import com.devcollab.dto.response.ScheduleResponseDTO;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.repository.ProjectScheduleRepository;
import com.devcollab.service.core.ProjectScheduleService;
import com.devcollab.service.system.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectScheduleServiceImpl implements ProjectScheduleService {

    private final ProjectRepository projectRepository;
    private final ProjectScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ScheduleResponseDTO createSchedule(Long projectId, ScheduleRequestDTO dto) {
        Project project = projectRepository.findByIdWithMembers(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        ProjectSchedule schedule = new ProjectSchedule();
        schedule.setProject(project);
        schedule.setTitle(dto.getTitle());
        schedule.setNote(dto.getNote());
        schedule.setMeetingLink(dto.getMeetingLink());
        schedule.setDatetime(convertToInstant(dto.getDatetime()));
        schedule.setCreatedAt(Instant.now());

        schedule = scheduleRepository.save(schedule);

        User creator = project.getCreatedBy();
        notificationService.notifyScheduleCreated(project, creator, schedule);

        return toDTO(schedule);
    }

    @Override
    public List<ScheduleResponseDTO> getSchedules(Long projectId) {
        return scheduleRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ScheduleResponseDTO getScheduleById(Long scheduleId) {
        ProjectSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        return toDTO(schedule);
    }

    @Override
    public ScheduleResponseDTO updateSchedule(Long scheduleId, ScheduleRequestDTO dto) {
        ProjectSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setTitle(dto.getTitle());
        schedule.setNote(dto.getNote());
        schedule.setMeetingLink(dto.getMeetingLink());
        schedule.setDatetime(convertToInstant(dto.getDatetime()));

        scheduleRepository.save(schedule);
        return toDTO(schedule);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new RuntimeException("Schedule not found");
        }
        scheduleRepository.deleteById(scheduleId);
    }

    @Override
    public List<ScheduleResponseDTO> getSchedulesByDate(Long projectId, LocalDate date) {
        ZoneId systemZone = ZoneId.systemDefault();
        Instant startOfDay = date.atStartOfDay(systemZone).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(systemZone).toInstant();

        return scheduleRepository.findByProject_ProjectIdAndDatetimeBetween(projectId, startOfDay, endOfDay)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<LocalDate> getScheduledDates(Long projectId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        ZoneId systemZone = ZoneId.systemDefault();
        Instant start = startDate.atStartOfDay(systemZone).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(systemZone).toInstant();

        return scheduleRepository.findByProject_ProjectIdAndDatetimeBetween(projectId, start, end)
                .stream()
                .map(schedule -> schedule.getDatetime().atZone(systemZone).toLocalDate())
                .distinct()
                .collect(Collectors.toList());
    }

    private Instant convertToInstant(String datetimeString) {
        try {
            // Parse as LocalDateTime and convert to Instant using system timezone
            LocalDateTime localDateTime = LocalDateTime.parse(datetimeString);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid datetime format. Expected: yyyy-MM-dd'T'HH:mm");
        }
    }

    private ScheduleResponseDTO toDTO(ProjectSchedule schedule) {
        ScheduleResponseDTO dto = new ScheduleResponseDTO();
        dto.setScheduleId(schedule.getId());
        dto.setProjectId(schedule.getProject().getProjectId());
        dto.setTitle(schedule.getTitle());
        dto.setNote(schedule.getNote());
        dto.setMeetingLink(schedule.getMeetingLink());
        dto.setDatetime(schedule.getDatetime());
        dto.setCreatedAt(schedule.getCreatedAt());
        return dto;
    }
}