package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.OperationsHistory;
import com.example.javadoc0032022.repository.OperationsHistoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
@AllArgsConstructor
public class OperationsHistoryController {

    private OperationsHistoryRepository operationsHistoryRepository;

    @GetMapping
    public ResponseEntity<?> getAllOperations(@RequestParam(defaultValue = "0") int page) {
        Page<OperationsHistory> pageTuts = operationsHistoryRepository.findAll(PageRequest.of(page, 10));
        return ResponseEntity.ok(pageTuts.getContent());
    }
}
