package com.devcollab.service.system;

import com.devcollab.domain.ProjectTarget;
import java.util.List;
import java.util.Optional;

public interface ProjectTargetService {

    ProjectTarget saveOrUpdateTarget(int month, int year, int targetCount, Long createdBy);

    List<ProjectTarget> getTargetsByYearAndPm(int year, Long pmId);

    Optional<ProjectTarget> getTargetByMonthYearAndPm(int month, int year, Long pmId);
}
