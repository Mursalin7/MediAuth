package com.mediauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "Decision is required")
    private String decision;  // APPROVED or REJECTED

    @NotBlank(message = "Remarks are required")
    private String remarks;
}
