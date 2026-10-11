package com.mediauth.service;

import com.mediauth.dto.*;
import com.mediauth.entity.*;
import com.mediauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository documentRepository;
    private final ClaimHistoryRepository historyRepository;
    private final GroqDecisionService groqDecisionService;
    private final PolicyPlanRepository policyPlanRepository;

    @Transactional
    public ClaimResponse submitClaim(ClaimSubmitRequest request, Patient patient) {
        PolicyPlan plan = policyPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan not found"));
        if (patient.getPolicy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your account must be linked to an insurance policy before submitting a claim");
        }
        if (request.getAdmissionDate().isBefore(request.getPolicyStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Admission date cannot be before policy start date");
        }

        long policyAgeDays = ChronoUnit.DAYS.between(request.getPolicyStartDate(), request.getAdmissionDate());
        boolean initialWaitingFailed = policyAgeDays < plan.getInitialWaitingDays()
                && !"EMERGENCY".equals(request.getHospitalizationType())
                && !"DAY_CARE".equals(request.getHospitalizationType());
        long sumInsured = plan.getSumInsured().longValue();
        double remaining = Math.max(0, sumInsured - request.getAmountClaimedThisYear());
        boolean specificWaitingFailed = plan.getSpecificIllnessWaitingMonths() > 0
                && java.time.YearMonth.from(request.getPolicyStartDate())
                .plusMonths(plan.getSpecificIllnessWaitingMonths())
                .isAfter(java.time.YearMonth.from(request.getAdmissionDate()))
                && matchesSpecificIllness(plan.getWording(), request.getDiagnosis());
        boolean preExistingWaitingFailed = Boolean.TRUE.equals(request.getPreExistingRelated())
                && java.time.YearMonth.from(request.getPolicyStartDate())
                .plusMonths(plan.getPedWaitingMonths())
                .isAfter(java.time.YearMonth.from(request.getAdmissionDate()));
        String exclusion = findExclusionMatch(plan.getExclusions(), request.getDiagnosis(), request.getTreatment());
        List<String> precheckFailures = new java.util.ArrayList<>();
        if (initialWaitingFailed) precheckFailures.add("Initial waiting period has not elapsed");
        if (specificWaitingFailed) precheckFailures.add("Specific illness waiting period has not elapsed");
        if (preExistingWaitingFailed) precheckFailures.add("Pre-existing condition waiting period has not elapsed");
        if (remaining <= 0) precheckFailures.add("Sum insured is exhausted");
        if (request.getEstimatedCost() > remaining) precheckFailures.add("Estimated cost exceeds remaining sum insured");
        if (exclusion != null) precheckFailures.add("Possible exclusion keyword match: " + exclusion);

        Claim claim = Claim.builder()
                .patient(patient)
                .policy(patient.getPolicy())
                .plan(plan)
                .claimType(request.getHospitalizationType())
                .hospitalizationType(request.getHospitalizationType())
                .diagnosis(request.getDiagnosis())
                .procedure(request.getTreatment())
                .details(request.getDetails())
                .estimatedAmount(request.getEstimatedCost())
                .admissionDate(request.getAdmissionDate())
                .policyStartDate(request.getPolicyStartDate())
                .amountClaimedThisYear(request.getAmountClaimedThisYear())
                .preExistingRelated(request.getPreExistingRelated())
                .payableAmount(Math.min(request.getEstimatedCost(), remaining) * (1 - plan.getCopayPercent() / 100.0))
                .status(ClaimStatus.SUBMITTED)
                .submittedAt(LocalDateTime.now())
                .build();

        claim = claimRepository.save(claim);

        recordHistory(claim, null, ClaimStatus.SUBMITTED, patient.getEmail(), "Claim submitted");

        // Run AI decisioning asynchronously-inline (simple approach for portfolio)
        try {
            groqDecisionService.processDecision(claim, patient, policyAgeDays, remaining, initialWaitingFailed,
                    specificWaitingFailed, preExistingWaitingFailed,
                    exclusion, precheckFailures);
        } catch (Exception e) {
            log.error("AI decisioning failed for claim {}: {}", claim.getId(), e.getMessage(), e);
            claim.setStatus(ClaimStatus.NEEDS_HUMAN_REVIEW);
            claim.setAiDecision("NEEDS_HUMAN_REVIEW");
            claim.setAiRationale("AI decisioning failed; routed to human review.");
            claim.setAiConfidence(0.0);
            claim.setMissingInformation("AI decision unavailable");
            claimRepository.save(claim);
            recordHistory(claim, ClaimStatus.SUBMITTED, ClaimStatus.NEEDS_HUMAN_REVIEW,
                    "SYSTEM", "AI decisioning failed, routed to human review");
        }

        return toResponse(claimRepository.findById(claim.getId()).orElseThrow());
    }

    private String findExclusionMatch(String exclusions, String diagnosis, String treatment) {
        if (exclusions == null || exclusions.isBlank()) return null;
        String combined = (diagnosis + " " + treatment).toLowerCase(java.util.Locale.ROOT);
        for (String keyword : exclusions.split(",")) {
            String candidate = keyword.trim();
            if (!candidate.isEmpty() && combined.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private boolean matchesSpecificIllness(String wording, String diagnosis) {
        String text = wording.toLowerCase(java.util.Locale.ROOT);
        String illness = diagnosis.toLowerCase(java.util.Locale.ROOT);
        String[] knownTerms = {"cataract", "hernia", "piles", "kidney stones", "joint replacement", "hysterectomy", "tonsillectomy"};
        for (String term : knownTerms) {
            if (illness.contains(term) && text.contains(term)) return true;
        }
        return false;
    }

    public ClaimResponse getClaim(Long id, Patient requestingPatient) {
        Claim claim = findClaimWithAccess(id, requestingPatient);
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse reviewClaim(Long id, ReviewRequest request, String reviewerEmail) {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        ClaimStatus newStatus = switch (request.getDecision().toUpperCase()) {
            case "APPROVED" -> ClaimStatus.APPROVED;
            case "REJECTED" -> ClaimStatus.REJECTED;
            case "SETTLED" -> ClaimStatus.SETTLED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid decision. Must be APPROVED, REJECTED, or SETTLED");
        };

        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(newStatus);
        claim.setDecidedAt(LocalDateTime.now());
        claimRepository.save(claim);

        recordHistory(claim, previousStatus, newStatus, reviewerEmail, request.getRemarks());

        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse addDocument(Long claimId, DocumentUploadRequest request, Patient patient) {
        Claim claim = findClaimWithAccess(claimId, patient);

        ClaimDocument doc = ClaimDocument.builder()
                .claim(claim)
                .docType(request.getDocType())
                .fileUrl(request.getFileUrl())
                .uploadedAt(LocalDateTime.now())
                .build();
        documentRepository.save(doc);

        return toResponse(claim);
    }

    public Page<ClaimResponse> listClaims(Patient requestingPatient, boolean isReviewer,
                                           ClaimStatus statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<Claim> claims;

        if (isReviewer) {
            claims = statusFilter != null
                    ? claimRepository.findByStatus(statusFilter, pageable)
                    : claimRepository.findAll(pageable);
        } else {
            claims = statusFilter != null
                    ? claimRepository.findByPatientAndStatus(requestingPatient, statusFilter, pageable)
                    : claimRepository.findByPatient(requestingPatient, pageable);
        }

        return claims.map(this::toResponse);
    }

    // --- Helpers ---

    private Claim findClaimWithAccess(Long id, Patient patient) {
        if (patient.getRole() == UserRole.REVIEWER) {
            return claimRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));
        }
        return claimRepository.findByIdAndPatient(id, patient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Claim not found or access denied"));
    }

    private void recordHistory(Claim claim, ClaimStatus from, ClaimStatus to, String actor, String remarks) {
        ClaimHistory history = ClaimHistory.builder()
                .claim(claim)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .remarks(remarks)
                .timestamp(LocalDateTime.now())
                .build();
        historyRepository.save(history);
    }

    public ClaimResponse toResponse(Claim claim) {
        List<ClaimResponse.DocumentResponse> docs = documentRepository
                .findByClaimId(claim.getId())
                .stream()
                .map(d -> ClaimResponse.DocumentResponse.builder()
                        .id(d.getId())
                        .docType(d.getDocType())
                        .fileUrl(d.getFileUrl())
                        .uploadedAt(d.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        List<ClaimResponse.HistoryResponse> history = historyRepository
                .findByClaimIdOrderByTimestampAsc(claim.getId())
                .stream()
                .map(h -> ClaimResponse.HistoryResponse.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                        .toStatus(h.getToStatus().name())
                        .actor(h.getActor())
                        .remarks(h.getRemarks())
                        .timestamp(h.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return ClaimResponse.builder()
                .id(claim.getId())
                .patientId(claim.getPatient().getId())
                .patientName(claim.getPatient().getName())
                .policyId(claim.getPolicy() == null ? null : claim.getPolicy().getId())
                .policyNumber(claim.getPolicy() == null ? null : claim.getPolicy().getPolicyNumber())
                .planId(claim.getPlan() == null ? null : claim.getPlan().getId())
                .planName(claim.getPlan() == null ? null : claim.getPlan().getPlanName())
                .payableAmount(java.math.BigDecimal.valueOf(claim.getPayableAmount() == null ? 0 : claim.getPayableAmount()))
                .claimType(claim.getClaimType())
                .hospitalizationType(claim.getHospitalizationType())
                .diagnosis(claim.getDiagnosis())
                .procedure(claim.getProcedure())
                .estimatedAmount(claim.getEstimatedAmount())
                .details(claim.getDetails())
                .admissionDate(claim.getAdmissionDate())
                .policyStartDate(claim.getPolicyStartDate())
                .amountClaimedThisYear(claim.getAmountClaimedThisYear())
                .preExistingRelated(claim.getPreExistingRelated())
                .status(claim.getStatus())
                .aiDecision(claim.getAiDecision())
                .aiRationale(claim.getAiRationale())
                .aiConfidence(claim.getAiConfidence())
                .clausesCited(claim.getClausesCited() == null || claim.getClausesCited().isBlank()
                        ? List.of() : List.of(claim.getClausesCited().split("\\|")))
                .missingInformation(claim.getMissingInformation() == null || claim.getMissingInformation().isBlank()
                        ? List.of() : List.of(claim.getMissingInformation().split("\\|")))
                .submittedAt(claim.getSubmittedAt())
                .decidedAt(claim.getDecidedAt())
                .documents(docs)
                .history(history)
                .build();
    }
}
