package com.mediauth.dto;

import com.mediauth.entity.PolicyPlan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlanResponse {
    private Long id;
    private String category;
    private String planName;
    private BigDecimal sumInsured;
    private Integer copayPercent;
    private String restoration;

    public static PlanResponse from(PolicyPlan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .category(plan.getCategory())
                .planName(plan.getPlanName())
                .sumInsured(plan.getSumInsured())
                .copayPercent(plan.getCopayPercent())
                .restoration(plan.getRestoration())
                .build();
    }
}
