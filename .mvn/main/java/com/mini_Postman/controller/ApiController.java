package com.mini_Postman.controller;

import com.mini_Postman.dto.ApiRequestDto;
import com.mini_Postman.dto.ApiResponse;
import com.mini_Postman.dto.ApiResponseDto;
import com.mini_Postman.model.HistoryItem;
import com.mini_Postman.service.ApiTestingService;
import com.mini_Postman.service.HistoryStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ApiTestingService apiTestingService;
    private final HistoryStorageService historyStorageService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ApiResponseDto>> sendRequest(@Valid @RequestBody ApiRequestDto requestDto) {
        // Ensure URL is complete
        if (!requestDto.getUrl().startsWith("http://") && !requestDto.getUrl().startsWith("https://")) {
            requestDto.setUrl("http://" + requestDto.getUrl());
        }
        
        ApiResponseDto result = apiTestingService.executeRequest(requestDto);
        return ResponseEntity.ok(ApiResponse.success("Request executed successfully", result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HistoryItem>>> getHistory() {
        List<HistoryItem> history = historyStorageService.getAllHistory();
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<HistoryItem>> getHistoryItem(@PathVariable String id) {
        Optional<HistoryItem> item = historyStorageService.getHistoryItem(id);
        return item.map(historyItem -> ResponseEntity.ok(ApiResponse.success(historyItem)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(ApiResponse.error("History item not found")));
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistoryItem(@PathVariable String id) {
        boolean deleted = historyStorageService.deleteHistoryItem(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("History item deleted", null));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error("History item not found"));
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        historyStorageService.clearAllHistory();
        return ResponseEntity.ok(ApiResponse.success("All history cleared", null));
    }
}
