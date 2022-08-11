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

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@AllArgsConstructor
public class OperationsHistoryController {

    private OperationsHistoryRepository operationsHistoryRepository;

    @GetMapping
    public ResponseEntity<?> getAllOperations(@RequestParam(defaultValue = "0") int page) {
        Page<OperationsHistory> pageTuts = operationsHistoryRepository.findAll(PageRequest.of(page, 10));

        Map<String, Object> response = new HashMap<>();
        response.put("packages", pageTuts.getContent());
        response.put("currentPage", pageTuts.getNumber());
        response.put("totalItems", pageTuts.getTotalElements());
        response.put("totalPages", pageTuts.getTotalPages());

        return ResponseEntity.ok(response);
    }
}
