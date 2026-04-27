package com.mini_Postman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultDto {

    private String testName;
    private String assertionType;
    private String expectedValue;
    private String actualValue;
    private boolean passed;
    private String message;
}
