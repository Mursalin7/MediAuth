package com.mediauth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClaimSubmitRequest {

    @NotNull(message = "Plan is required")
    @Positive(message = "Plan ID must be positive")
    private Long planId;

    @NotBlank(message = "Hospitalization type is required")
    @Pattern(regexp = "PLANNED|EMERGENCY|DAY_CARE", message = "Invalid hospitalization type")
    private String hospitalizationType;

    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    @NotBlank(message = "Treatment is required")
    private String treatment;

    @NotNull(message = "Estimated cost is required")
    @Positive(message = "Estimated cost must be greater than zero")
    private Double estimatedCost;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;

    @NotNull(message = "Policy start date is required")
    private LocalDate policyStartDate;

    @NotNull(message = "Amount claimed this year is required")
    @PositiveOrZero(message = "Amount claimed must be zero or more")
    private Double amountClaimedThisYear;

    @NotNull(message = "Pre-existing related flag is required")
    private Boolean preExistingRelated;

    private String details;
}
