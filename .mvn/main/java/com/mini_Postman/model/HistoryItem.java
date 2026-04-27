package com.mini_Postman.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {

    private String id;
    private String url;
    private String method;
    private Map<String, String> headers;
    private String body;
    private Map<String, String> queryParams;
    private int statusCode;
    private String statusText;
    private long responseTimeMs;
    private long responseSizeBytes;
    private LocalDateTime createdAt;
    private List<TestResultSummary> testResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResultSummary {
        private String testName;
        private boolean passed;
    }
}
