package com.mini_Postman.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {

    @NotBlank(message = "Test name is required")
    private String testName;

    @NotBlank(message = "Assertion type is required")
    private String assertionType; // STATUS_CODE_EQUALS, RESPONSE_TIME_LESS_THAN, BODY_CONTAINS, BODY_HAS_JSON_FIELD

    @NotBlank(message = "Expected value is required")
    private String expectedValue;
}
