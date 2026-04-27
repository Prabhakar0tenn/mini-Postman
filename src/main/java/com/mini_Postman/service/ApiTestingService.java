package com.mini_Postman.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini_Postman.dto.ApiRequestDto;
import com.mini_Postman.dto.ApiResponseDto;
import com.mini_Postman.dto.TestCaseDto;
import com.mini_Postman.dto.TestResultDto;
import com.mini_Postman.exception.ApiException;
import com.mini_Postman.model.HistoryItem;
import com.mini_Postman.util.HttpStatusUtil;
import com.mini_Postman.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiTestingService {

    private final WebClient webClient;
    private final HistoryStorageService historyStorageService;
    private final ObjectMapper objectMapper;

    public ApiResponseDto executeRequest(ApiRequestDto requestDto) {
        long startTime = System.currentTimeMillis();
        ApiResponseDto responseDto = new ApiResponseDto();

        try {
            HttpMethod method = HttpMethod.valueOf(requestDto.getMethod().toUpperCase());

            org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(requestDto.getUrl());
            if (requestDto.getQueryParams() != null) {
                requestDto.getQueryParams().forEach(builder::queryParam);
            }

            WebClient.RequestBodySpec requestSpec = webClient.method(method)
                    .uri(builder.build().toUri());

            // Add Headers
            if (requestDto.getHeaders() != null) {
                requestDto.getHeaders().forEach(requestSpec::header);
            }

            // Add Body
            if (requestDto.getBody() != null && !requestDto.getBody().trim().isEmpty() &&
                    (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
                requestSpec.bodyValue(requestDto.getBody());
            }

            // Execute Request
            ResponseEntity<String> responseEntity = requestSpec.exchangeToMono(clientResponse -> 
                    clientResponse.toEntity(String.class)
            ).block(Duration.ofSeconds(30)); // Max wait 30 seconds

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            populateResponseSuccess(responseEntity, responseDto, duration);

        } catch (WebClientResponseException e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            populateResponseError(e, responseDto, duration);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            responseDto.setStatusCode(500);
            responseDto.setStatusText("Internal Server Error");
            responseDto.setError(e.getMessage());
            responseDto.setResponseTimeMs(duration);
        }

        // Run Tests if any
        if (requestDto.getTestCases() != null && !requestDto.getTestCases().isEmpty()) {
            List<TestResultDto> testResults = evaluateTests(requestDto.getTestCases(), responseDto);
            responseDto.setTestResults(testResults);
        } else {
            responseDto.setTestResults(new ArrayList<>());
        }

        // Save to History
        saveToHistory(requestDto, responseDto);

        return responseDto;
    }

    private void populateResponseSuccess(ResponseEntity<String> responseEntity, ApiResponseDto responseDto, long duration) {
        if (responseEntity != null) {
            HttpStatusCode statusCode = responseEntity.getStatusCode();
            responseDto.setStatusCode(statusCode.value());
            responseDto.setStatusText(HttpStatusUtil.getReasonPhrase(statusCode));
            
            Map<String, List<String>> headers = new HashMap<>();
            responseEntity.getHeaders().forEach(headers::put);
            responseDto.setResponseHeaders(headers);

            String body = responseEntity.getBody();
            if (body != null) {
                responseDto.setResponseBody(body);
                responseDto.setResponseSizeBytes(body.getBytes().length);
                
                // Format JSON if possible
                try {
                    JsonNode jsonNode = objectMapper.readTree(body);
                    responseDto.setResponseBody(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
                } catch (JsonProcessingException ignored) {
                    // Not valid JSON, keep raw string
                }
            } else {
                responseDto.setResponseBody("");
                responseDto.setResponseSizeBytes(0);
            }
        }
        responseDto.setResponseTimeMs(duration);
    }

    private void populateResponseError(WebClientResponseException e, ApiResponseDto responseDto, long duration) {
        responseDto.setStatusCode(e.getStatusCode().value());
        responseDto.setStatusText(HttpStatusUtil.getReasonPhrase(e.getStatusCode()));
        
        Map<String, List<String>> headers = new HashMap<>();
        e.getHeaders().forEach(headers::put);
        responseDto.setResponseHeaders(headers);
        
        String body = e.getResponseBodyAsString();
        responseDto.setResponseBody(body);
        responseDto.setResponseSizeBytes(body != null ? body.getBytes().length : 0);
        responseDto.setError(e.getMessage());
        responseDto.setResponseTimeMs(duration);
    }

    private List<TestResultDto> evaluateTests(List<TestCaseDto> testCases, ApiResponseDto responseDto) {
        List<TestResultDto> results = new ArrayList<>();
        
        for (TestCaseDto testCase : testCases) {
            TestResultDto result = new TestResultDto();
            result.setTestName(testCase.getTestName());
            result.setAssertionType(testCase.getAssertionType());
            result.setExpectedValue(testCase.getExpectedValue());
            
            try {
                switch (testCase.getAssertionType()) {
                    case "STATUS_CODE_EQUALS":
                        int expectedStatus = Integer.parseInt(testCase.getExpectedValue());
                        result.setActualValue(String.valueOf(responseDto.getStatusCode()));
                        result.setPassed(expectedStatus == responseDto.getStatusCode());
                        result.setMessage(result.isPassed() ? "Status code matches" : "Status code mismatch");
                        break;
                        
                    case "RESPONSE_TIME_LESS_THAN":
                        long expectedTime = Long.parseLong(testCase.getExpectedValue());
                        result.setActualValue(String.valueOf(responseDto.getResponseTimeMs()));
                        result.setPassed(responseDto.getResponseTimeMs() < expectedTime);
                        result.setMessage(result.isPassed() ? "Response time within limit" : "Response time exceeded limit");
                        break;
                        
                    case "BODY_CONTAINS":
                        String actualBody = responseDto.getResponseBody() != null ? responseDto.getResponseBody() : "";
                        result.setActualValue(actualBody.length() > 50 ? actualBody.substring(0, 50) + "..." : actualBody);
                        result.setPassed(actualBody.contains(testCase.getExpectedValue()));
                        result.setMessage(result.isPassed() ? "Body contains string" : "Body does not contain string");
                        break;
                        
                    case "BODY_HAS_JSON_FIELD":
                        if (responseDto.getResponseBody() == null) {
                            result.setPassed(false);
                            result.setMessage("Response body is empty");
                            result.setActualValue("null");
                            break;
                        }
                        try {
                            JsonNode node = objectMapper.readTree(responseDto.getResponseBody());
                            String fieldPath = testCase.getExpectedValue();
                            
                            // Simple path evaluation like "data.user.id" -> ignoring complex JSONPath for MVP
                            String[] paths = fieldPath.split("\\.");
                            JsonNode current = node;
                            boolean found = true;
                            for (String path : paths) {
                                if (current.has(path)) {
                                    current = current.get(path);
                                } else {
                                    found = false;
                                    break;
                                }
                            }
                            
                            result.setPassed(found);
                            result.setActualValue(found ? current.toString() : "field not found");
                            result.setMessage(result.isPassed() ? "JSON field exists" : "JSON field missing");
                            
                        } catch (Exception e) {
                            result.setPassed(false);
                            result.setActualValue("Invalid JSON");
                            result.setMessage("Failed to parse JSON body");
                        }
                        break;
                        
                    default:
                        result.setPassed(false);
                        result.setMessage("Unknown assertion type: " + testCase.getAssertionType());
                }
            } catch (Exception e) {
                result.setPassed(false);
                result.setMessage("Error evaluating test: " + e.getMessage());
                result.setActualValue("Error");
            }
            
            results.add(result);
        }
        
        return results;
    }

    private void saveToHistory(ApiRequestDto requestDto, ApiResponseDto responseDto) {
        HistoryItem historyItem = HistoryItem.builder()
                .id(IdGenerator.generateId())
                .url(requestDto.getUrl())
                .method(requestDto.getMethod())
                .headers(requestDto.getHeaders())
                .body(requestDto.getBody())
                .queryParams(requestDto.getQueryParams())
                .statusCode(responseDto.getStatusCode())
                .statusText(responseDto.getStatusText())
                .responseTimeMs(responseDto.getResponseTimeMs())
                .responseSizeBytes(responseDto.getResponseSizeBytes())
                .createdAt(LocalDateTime.now())
                .build();
                
        if (responseDto.getTestResults() != null) {
            List<HistoryItem.TestResultSummary> summaries = responseDto.getTestResults().stream()
                    .map(tr -> new HistoryItem.TestResultSummary(tr.getTestName(), tr.isPassed()))
                    .collect(Collectors.toList());
            historyItem.setTestResults(summaries);
        }
        
        historyStorageService.saveHistoryItem(historyItem);
    }
}
