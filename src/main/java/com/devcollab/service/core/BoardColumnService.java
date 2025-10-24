package com.devcollab.service.core;

import com.devcollab.domain.BoardColumn;

import java.util.List;

public interface BoardColumnService {

    List<BoardColumn> getColumnsByProject(Long projectId);


    BoardColumn getById(Long columnId);


    BoardColumn save(BoardColumn column);

    void delete(Long columnId);
}
