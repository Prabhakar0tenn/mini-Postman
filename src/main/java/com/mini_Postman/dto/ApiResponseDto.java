package com.mini_Postman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto {

    private int statusCode;
    private String statusText;
    private Map<String, List<String>> responseHeaders;
    private String responseBody;
    private long responseTimeMs;
    private long responseSizeBytes;
    private String error;
    private List<TestResultDto> testResults;
}
