package com.devcollab.service.impl.system;

import com.devcollab.domain.ProjectTarget;
import com.devcollab.repository.ProjectTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectTargetServiceImplTest {

    @Mock
    private ProjectTargetRepository projectTargetRepository;

    @InjectMocks
    private ProjectTargetServiceImpl projectTargetService;

    private ProjectTarget testTarget;
    private Long testPmId = 1L;
    private int testMonth = 11;
    private int testYear = 2024;

    @BeforeEach
    void setUp() {
        testTarget = ProjectTarget.builder()
                .id(1L)
                .month(testMonth)
                .year(testYear)
                .targetCount(10)
                .createdBy(testPmId)
                .build();
    }

    @Test
    void testSaveOrUpdateTarget_NewTarget() {
        // Given
        when(projectTargetRepository.findByMonthYearAndPm(testMonth, testYear, testPmId))
                .thenReturn(Optional.empty());
        when(projectTargetRepository.save(any(ProjectTarget.class))).thenReturn(testTarget);

        // When
        ProjectTarget result = projectTargetService.saveOrUpdateTarget(testMonth, testYear, 10, testPmId);

        // Then
        assertNotNull(result);
        verify(projectTargetRepository).findByMonthYearAndPm(testMonth, testYear, testPmId);
        verify(projectTargetRepository).save(any(ProjectTarget.class));
    }

    @Test
    void testSaveOrUpdateTarget_UpdateExisting() {
        // Given
        when(projectTargetRepository.findByMonthYearAndPm(testMonth, testYear, testPmId))
                .thenReturn(Optional.of(testTarget));
        when(projectTargetRepository.save(any(ProjectTarget.class))).thenReturn(testTarget);

        // When
        ProjectTarget result = projectTargetService.saveOrUpdateTarget(testMonth, testYear, 15, testPmId);

        // Then
        assertNotNull(result);
        assertEquals(15, testTarget.getTargetCount());
        verify(projectTargetRepository).findByMonthYearAndPm(testMonth, testYear, testPmId);
        verify(projectTargetRepository).save(testTarget);
    }

    @Test
    void testGetTargetsByYearAndPm_Success() {
        // Given
        List<ProjectTarget> targets = Arrays.asList(testTarget);
        when(projectTargetRepository.findTargetsByYearAndPm(testYear, testPmId))
                .thenReturn(targets);

        // When
        List<ProjectTarget> result = projectTargetService.getTargetsByYearAndPm(testYear, testPmId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(projectTargetRepository).findTargetsByYearAndPm(testYear, testPmId);
    }

    @Test
    void testGetTargetByMonthYearAndPm_Success() {
        // Given
        when(projectTargetRepository.findByMonthYearAndPm(testMonth, testYear, testPmId))
                .thenReturn(Optional.of(testTarget));

        // When
        Optional<ProjectTarget> result = projectTargetService.getTargetByMonthYearAndPm(testMonth, testYear, testPmId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testTarget, result.get());
        verify(projectTargetRepository).findByMonthYearAndPm(testMonth, testYear, testPmId);
    }

    @Test
    void testGetTargetByMonthYearAndPm_NotFound() {
        // Given
        when(projectTargetRepository.findByMonthYearAndPm(testMonth, testYear, testPmId))
                .thenReturn(Optional.empty());

        // When
        Optional<ProjectTarget> result = projectTargetService.getTargetByMonthYearAndPm(testMonth, testYear, testPmId);

        // Then
        assertFalse(result.isPresent());
        verify(projectTargetRepository).findByMonthYearAndPm(testMonth, testYear, testPmId);
    }
}

