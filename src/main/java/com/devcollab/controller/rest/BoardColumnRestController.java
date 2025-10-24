package com.devcollab.controller.rest;

import com.devcollab.dto.BoardColumnDTO;
import com.devcollab.service.core.BoardColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/columns")
public class BoardColumnRestController {

    private final BoardColumnService boardColumnService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<BoardColumnDTO>> getColumnsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                boardColumnService.getColumnsByProject(projectId)
                        .stream()
                        .map(BoardColumnDTO::fromEntity)
                        .toList());
    }


    @GetMapping("/{columnId}")
    public ResponseEntity<BoardColumnDTO> getColumnById(@PathVariable Long columnId) {
        var col = boardColumnService.getById(columnId);
        return (col != null)
                ? ResponseEntity.ok(BoardColumnDTO.fromEntity(col))
                : ResponseEntity.notFound().build();
    }


    @PostMapping
    public ResponseEntity<BoardColumnDTO> saveColumn(@RequestBody BoardColumnDTO dto) {
        var entity = boardColumnService.save(dto.toEntity());
        return ResponseEntity.ok(BoardColumnDTO.fromEntity(entity));
    }


    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable Long columnId) {
        boardColumnService.delete(columnId);
        return ResponseEntity.noContent().build();
    }
}
