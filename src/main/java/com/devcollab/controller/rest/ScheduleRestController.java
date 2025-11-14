package com.devcollab.controller.rest;

import com.devcollab.domain.ProjectSchedule;
import com.devcollab.dto.request.ScheduleRequestDTO;
import com.devcollab.dto.response.ScheduleResponseDTO;
import com.devcollab.service.core.ProjectScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ScheduleRestController {

    private final ProjectScheduleService scheduleService;

    @PostMapping("/{projectId}/schedule")
    public ResponseEntity<?> createSchedule(
            @PathVariable Long projectId,
            @RequestBody ScheduleRequestDTO dto) {
        try {
            ScheduleResponseDTO saved = scheduleService.createSchedule(projectId, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{projectId}/schedule")
    public ResponseEntity<?> getSchedules(@PathVariable Long projectId) {
        return ResponseEntity.ok(scheduleService.getSchedules(projectId));
    }

    @GetMapping("/{projectId}/schedule/dates")
    public ResponseEntity<?> getScheduledDates(
            @PathVariable Long projectId,
            @RequestParam int year,
            @RequestParam int month) {
        try {
            List<LocalDate> scheduledDates = scheduleService.getScheduledDates(projectId, year, month);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", scheduledDates
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    @GetMapping("/{projectId}/schedule/{scheduleId}")
    public ResponseEntity<?> getScheduleById(
            @PathVariable Long projectId,
            @PathVariable Long scheduleId) {
        try {
            ScheduleResponseDTO schedule = scheduleService.getScheduleById(scheduleId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", schedule
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    @PutMapping("/{projectId}/schedule/{scheduleId}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable Long projectId,
            @PathVariable Long scheduleId,
            @RequestBody ScheduleRequestDTO dto) {
        try {
            ScheduleResponseDTO updated = scheduleService.updateSchedule(scheduleId, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{projectId}/schedule/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long projectId,
            @PathVariable Long scheduleId) {
        try {
            scheduleService.deleteSchedule(scheduleId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Schedule deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{projectId}/schedule/date/{date}")
    public ResponseEntity<?> getSchedulesByDate(
            @PathVariable Long projectId,
            @PathVariable String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<ScheduleResponseDTO> schedules = scheduleService.getSchedulesByDate(projectId, localDate);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", schedules
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}