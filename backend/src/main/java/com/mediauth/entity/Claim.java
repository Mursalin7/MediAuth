package com.mediauth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private PolicyPlan plan;

    // Retained for compatibility with existing database rows and schemas.
    @Column(name = "claim_type", nullable = false, columnDefinition = "varchar(255) default 'PLANNED'")
    private String claimType;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'PLANNED'")
    private String hospitalizationType;

    @Column(nullable = false)
    private String diagnosis;

    @Column(nullable = false)
    private String procedure;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Double estimatedAmount;

    @Column(nullable = false, columnDefinition = "date default current_date")
    private java.time.LocalDate admissionDate;

    @Column(nullable = false, columnDefinition = "date default current_date")
    private java.time.LocalDate policyStartDate;

    @Column(nullable = false, columnDefinition = "float8 default 0")
    private Double amountClaimedThisYear;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean preExistingRelated;

    private Double payableAmount;

    @Column(columnDefinition = "TEXT")
    private String clausesCited;

    @Column(columnDefinition = "TEXT")
    private String missingInformation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(columnDefinition = "TEXT")
    private String aiDecision;

    @Column(columnDefinition = "TEXT")
    private String aiRationale;

    private Double aiConfidence;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;
}
