package com.mini_Postman.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class ApiRequestDto {

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "HTTP method is required")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "Method must be GET, POST, PUT, DELETE, or PATCH")
    private String method;

    private Map<String, String> headers;

    private String body;

    private Map<String, String> queryParams;

    private List<TestCaseDto> testCases;
}
