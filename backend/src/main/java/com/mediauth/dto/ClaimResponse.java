package com.mediauth.dto;

import com.mediauth.entity.ClaimStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ClaimResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long policyId;
    private String policyNumber;
    private Long planId;
    private String planName;
    private BigDecimal payableAmount;
    private String claimType;
    private String hospitalizationType;
    private String diagnosis;
    private String procedure;
    private Double estimatedAmount;
    private String details;
    private LocalDate admissionDate;
    private LocalDate policyStartDate;
    private Double amountClaimedThisYear;
    private Boolean preExistingRelated;
    private ClaimStatus status;
    private String aiDecision;
    private String aiRationale;
    private Double aiConfidence;
    private List<String> clausesCited;
    private List<String> missingInformation;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private List<DocumentResponse> documents;
    private List<HistoryResponse> history;

    @Data
    @Builder
    public static class DocumentResponse {
        private Long id;
        private String docType;
        private String fileUrl;
        private LocalDateTime uploadedAt;
    }

    @Data
    @Builder
    public static class HistoryResponse {
        private Long id;
        private String fromStatus;
        private String toStatus;
        private String actor;
        private String remarks;
        private LocalDateTime timestamp;
    }
}
