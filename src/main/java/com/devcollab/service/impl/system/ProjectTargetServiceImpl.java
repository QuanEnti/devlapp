package com.devcollab.service.impl.system;

import com.devcollab.domain.ProjectTarget;
import com.devcollab.repository.ProjectTargetRepository;
import com.devcollab.service.system.ProjectTargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectTargetServiceImpl implements ProjectTargetService {

    private final ProjectTargetRepository projectTargetRepository;

    @Override
    public ProjectTarget saveOrUpdateTarget(int month, int year, int targetCount, Long createdBy) {
        Optional<ProjectTarget> existing = projectTargetRepository.findByMonthYearAndPm(month, year, createdBy);

        if (existing.isPresent()) {
            ProjectTarget pt = existing.get();
            pt.setTargetCount(targetCount);
            return projectTargetRepository.save(pt);
        }

        ProjectTarget newTarget = ProjectTarget.builder()
                .month(month)
                .year(year)
                .targetCount(targetCount)
                .createdBy(createdBy)
                .build();
        return projectTargetRepository.save(newTarget);
    }

    @Override
    public List<ProjectTarget> getTargetsByYearAndPm(int year, Long pmId) {
        return projectTargetRepository.findTargetsByYearAndPm(year, pmId);
    }

    @Override
    public Optional<ProjectTarget> getTargetByMonthYearAndPm(int month, int year, Long pmId) {
        return projectTargetRepository.findByMonthYearAndPm(month, year, pmId);
    }
}
