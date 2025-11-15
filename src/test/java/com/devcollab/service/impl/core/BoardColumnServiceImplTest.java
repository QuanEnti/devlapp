package com.devcollab.service.impl.core;

import com.devcollab.domain.BoardColumn;
import com.devcollab.repository.BoardColumnRepository;
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
class BoardColumnServiceImplTest {

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @InjectMocks
    private BoardColumnServiceImpl boardColumnService;

    private BoardColumn testColumn;
    private Long testColumnId = 1L;
    private Long testProjectId = 100L;

    @BeforeEach
    void setUp() {
        testColumn = new BoardColumn();
        testColumn.setColumnId(testColumnId);
        testColumn.setName("To-do");
        testColumn.setOrderIndex(1);
    }

    @Test
    void testGetColumnsByProject_Success() {
        // Given
        List<BoardColumn> expectedColumns = Arrays.asList(testColumn);
        when(boardColumnRepository.findByProject_ProjectIdOrderByOrderIndexAsc(testProjectId))
                .thenReturn(expectedColumns);

        // When
        List<BoardColumn> result = boardColumnService.getColumnsByProject(testProjectId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testColumn, result.get(0));
        verify(boardColumnRepository).findByProject_ProjectIdOrderByOrderIndexAsc(testProjectId);
    }

    @Test
    void testGetColumnsByProject_EmptyList() {
        // Given
        when(boardColumnRepository.findByProject_ProjectIdOrderByOrderIndexAsc(testProjectId))
                .thenReturn(List.of());

        // When
        List<BoardColumn> result = boardColumnService.getColumnsByProject(testProjectId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(boardColumnRepository).findByProject_ProjectIdOrderByOrderIndexAsc(testProjectId);
    }

    @Test
    void testGetById_Success() {
        // Given
        when(boardColumnRepository.findById(testColumnId)).thenReturn(Optional.of(testColumn));

        // When
        BoardColumn result = boardColumnService.getById(testColumnId);

        // Then
        assertNotNull(result);
        assertEquals(testColumn, result);
        verify(boardColumnRepository).findById(testColumnId);
    }

    @Test
    void testGetById_NotFound() {
        // Given
        when(boardColumnRepository.findById(testColumnId)).thenReturn(Optional.empty());

        // When
        BoardColumn result = boardColumnService.getById(testColumnId);

        // Then
        assertNull(result);
        verify(boardColumnRepository).findById(testColumnId);
    }

    @Test
    void testSave_Success() {
        // Given
        when(boardColumnRepository.save(any(BoardColumn.class))).thenReturn(testColumn);

        // When
        BoardColumn result = boardColumnService.save(testColumn);

        // Then
        assertNotNull(result);
        assertEquals(testColumn, result);
        verify(boardColumnRepository).save(testColumn);
    }

    @Test
    void testDelete_Success() {
        // Given
        doNothing().when(boardColumnRepository).deleteById(testColumnId);

        // When
        boardColumnService.delete(testColumnId);

        // Then
        verify(boardColumnRepository).deleteById(testColumnId);
    }
}

