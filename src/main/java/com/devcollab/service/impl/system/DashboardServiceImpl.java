package com.devcollab.service.impl.system;

import com.devcollab.domain.ProjectTarget;
import com.devcollab.domain.User;
import com.devcollab.dto.response.ProjectPerformanceDTO;
import com.devcollab.dto.response.ProjectSummaryDTO;
import com.devcollab.repository.ProjectRepository;
import com.devcollab.service.impl.core.UserServiceImpl;
import com.devcollab.service.system.DashboardService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectTargetServiceImpl projectTargetService;
    private final UserServiceImpl userService;

    @Override
    public ProjectSummaryDTO getProjectSummary(String range) {

        LocalDateTime startDate = switch (range.toLowerCase()) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(6);
        };

        List<Object[]> stats = projectRepository.countProjectsByStatusSince(startDate);

        long total = 0, active = 0, completed = 0, onHold = 0, pending = 0, inProgress = 0;

        for (Object[] row : stats) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;

            switch (status.toUpperCase()) {
                case "ACTIVE" -> active = count;
                case "COMPLETED" -> completed = count;
                case "ON_HOLD" -> onHold = count;
                case "PENDING" -> pending = count;
                case "IN_PROGRESS" -> inProgress = count;
            }
        }

        return new ProjectSummaryDTO(total, active, completed, onHold, pending, inProgress);
    }

    @Override
    public ProjectSummaryDTO getProjectSummaryByPm(String range, String pmEmail) {
        LocalDateTime startDate = switch (range.toLowerCase()) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(6);
        };

        List<Object[]> stats =
                projectRepository.countProjectsByStatusSinceAndPm(pmEmail, startDate);

        long total = 0, active = 0, completed = 0, onHold = 0, pending = 0, inProgress = 0;

        for (Object[] row : stats) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;

            switch (status.toUpperCase()) {
                case "ACTIVE" -> active = count;
                case "COMPLETED" -> completed = count;
                case "ON_HOLD" -> onHold = count;
                case "PENDING" -> pending = count;
                case "IN_PROGRESS" -> inProgress = count;
            }
        }

        return new ProjectSummaryDTO(total, active, completed, onHold, pending, inProgress);
    }

    @Override
    public ProjectPerformanceDTO getProjectPerformance(String range) {
        Authentication auth =
                (Authentication) SecurityContextHolder.getContext().getAuthentication();
        Long pmId = getUserIdFromAuth(auth);

        LocalDateTime startDate = switch (range.toLowerCase()) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(6);
        };

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Get achieved from completed projects (auto-calculated)
        List<Object[]> results = projectRepository.countCompletedProjectsSince(startDate);
        Map<String, Long> achievedMap = new LinkedHashMap<>();
        results.forEach(r -> achievedMap.put((String) r[0], ((Number) r[1]).longValue()));

        // Get targets from database (manually set)
        List<ProjectTarget> targets = projectTargetService.getTargetsByYearAndPm(currentYear, pmId);
        Map<Integer, Integer> targetMap = new LinkedHashMap<>();
        targets.forEach(t -> targetMap.put(t.getMonth(), t.getTargetCount()));

        // If range is "year", show all 12 months of current year
        List<String> labels = new ArrayList<>();
        List<Long> achieved = new ArrayList<>();
        List<Long> target = new ArrayList<>();

        if ("year".equalsIgnoreCase(range)) {
            // Show all 12 months
            String[] monthNames = {"January", "February", "March", "April", "May", "June", "July",
                    "August", "September", "October", "November", "December"};
            for (int month = 1; month <= 12; month++) {
                String monthName = monthNames[month - 1];
                labels.add(monthName);
                // Get achieved for this month (0 if not found)
                achieved.add(achievedMap.getOrDefault(monthName, 0L));
                // Get target for this month (0 if not found)
                target.add((long) targetMap.getOrDefault(month, 0));
            }
        } else {
            // Use only months with data
            labels = new ArrayList<>(achievedMap.keySet());
            achieved = new ArrayList<>(achievedMap.values());
            for (String monthName : labels) {
                int month = monthNameToNumber(monthName);
                target.add((long) targetMap.getOrDefault(month, 0));
            }
        }

        return new ProjectPerformanceDTO(labels, achieved, target);
    }

    @Override
    public ProjectPerformanceDTO getProjectPerformanceByPm(String range, String pmEmail) {
        Long pmId = userService.getByEmail(pmEmail).map(User::getUserId).orElseThrow(
                () -> new RuntimeException("Không tìm thấy user với email: " + pmEmail));

        LocalDateTime startDate = switch (range.toLowerCase()) {
            case "week" -> LocalDateTime.now().minusWeeks(1);
            case "month" -> LocalDateTime.now().minusMonths(1);
            case "year" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.now().minusMonths(6);
        };

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // ✅ Get achieved from completed projects của PM (creator hoặc có role PM/ADMIN)
        List<Object[]> results =
                projectRepository.countCompletedProjectsSinceAndPm(pmEmail, startDate);
        Map<String, Long> achievedMap = new LinkedHashMap<>();
        results.forEach(r -> achievedMap.put((String) r[0], ((Number) r[1]).longValue()));

        // Get targets from database (manually set) của PM
        List<ProjectTarget> targets = projectTargetService.getTargetsByYearAndPm(currentYear, pmId);
        Map<Integer, Integer> targetMap = new LinkedHashMap<>();
        targets.forEach(t -> targetMap.put(t.getMonth(), t.getTargetCount()));

        // If range is "year", show all 12 months of current year
        List<String> labels = new ArrayList<>();
        List<Long> achieved = new ArrayList<>();
        List<Long> target = new ArrayList<>();

        if ("year".equalsIgnoreCase(range)) {
            // Show all 12 months
            String[] monthNames = {"January", "February", "March", "April", "May", "June", "July",
                    "August", "September", "October", "November", "December"};
            for (int month = 1; month <= 12; month++) {
                String monthName = monthNames[month - 1];
                labels.add(monthName);
                // Get achieved for this month (0 if not found)
                achieved.add(achievedMap.getOrDefault(monthName, 0L));
                // Get target for this month (0 if not found)
                target.add((long) targetMap.getOrDefault(month, 0));
            }
        } else {
            // Use only months with data
            labels = new ArrayList<>(achievedMap.keySet());
            achieved = new ArrayList<>(achievedMap.values());
            for (String monthName : labels) {
                int month = monthNameToNumber(monthName);
                target.add((long) targetMap.getOrDefault(month, 0));
            }
        }

        return new ProjectPerformanceDTO(labels, achieved, target);
    }

    private int monthNameToNumber(String name) {
        try {
            return Month.valueOf(name.toUpperCase()).getValue();
        } catch (Exception e) {
            return switch (name.toLowerCase()) {
                case "january" -> 1;
                case "february" -> 2;
                case "march" -> 3;
                case "april" -> 4;
                case "may" -> 5;
                case "june" -> 6;
                case "july" -> 7;
                case "august" -> 8;
                case "september" -> 9;
                case "october" -> 10;
                case "november" -> 11;
                case "december" -> 12;
                default -> 0;
            };
        }
    }

    private Long getUserIdFromAuth(Authentication auth) {
        String email;
        if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
            email = oauth2Auth.getPrincipal().getAttribute("email");
        } else {
            email = auth.getName();
        }

        return userService.getByEmail(email).map(User::getUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));
    }



}
