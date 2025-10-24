package com.devcollab.service.impl.core;

import com.devcollab.domain.BoardColumn;
import com.devcollab.repository.BoardColumnRepository;
import com.devcollab.service.core.BoardColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardColumnServiceImpl implements BoardColumnService {

    private final BoardColumnRepository boardColumnRepository;

    @Override
    public List<BoardColumn> getColumnsByProject(Long projectId) {
        return boardColumnRepository.findByProject_ProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Override
    public BoardColumn getById(Long columnId) {
        return boardColumnRepository.findById(columnId).orElse(null);
    }

    @Override
    public BoardColumn save(BoardColumn column) {
        return boardColumnRepository.save(column);
    }

    @Override
    public void delete(Long columnId) {
        boardColumnRepository.deleteById(columnId);
    }
}
