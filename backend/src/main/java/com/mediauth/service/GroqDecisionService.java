package com.mediauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediauth.entity.*;
import com.mediauth.repository.ClaimHistoryRepository;
import com.mediauth.repository.ClaimRepository;
import com.mediauth.entity.PolicyPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqDecisionService {

    private final ClaimRepository claimRepository;
    private final ClaimHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.groq.api-key}")
    private String groqApiKey;

    @Value("${app.groq.base-url}")
    private String groqBaseUrl;

    @Value("${app.groq.model}")
    private String groqModel;

    @PostConstruct
    void logConfiguration() {
        log.info("Groq decision service configured: apiKeyPresent={}, model={}",
                groqApiKey != null && !groqApiKey.isBlank(), groqModel);
    }

    /**
     * Sends the claim to Groq for AI decisioning and persists the result.
     */
    public void processDecision(Claim claim, Patient patient, long policyAgeDays, double remaining,
                                boolean initialWaitingFailed, boolean specificWaitingFailed,
                                boolean preExistingWaitingFailed, String exclusion,
                                List<String> precheckFailures) {
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new IllegalStateException("AI decisioning is disabled: configure GROQ_API_KEY to enable it");
        }

        String prompt = buildPrompt(claim, policyAgeDays, remaining, initialWaitingFailed,
                specificWaitingFailed, preExistingWaitingFailed, exclusion);

        Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 512
        );

        WebClient client = WebClient.builder()
                .baseUrl(groqBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String rawResponse;
        try {
            rawResponse = client.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("No response body")
                            .map(body -> new GroqApiException(response.statusCode().value(), body)))
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(55));
        } catch (Exception e) {
            log.error("Groq request failed for claim {}: {}", claim.getId(), safeErrorMessage(e));
            persistFallback(claim, "AI service timed out or was unavailable; routed to human review.",
                    List.of("AI response unavailable"), 0.0, "AI service unavailable");
            return;
        }

        parseAndPersistDecision(claim, rawResponse, precheckFailures);
    }

    private String safeErrorMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof GroqApiException apiError) {
                return "HTTP " + apiError.statusCode + ": " + apiError.responseBody;
            }
            current = current.getCause();
        }
        return error.getClass().getSimpleName() + ": " + error.getMessage();
    }

    private static class GroqApiException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        private GroqApiException(int statusCode, String responseBody) {
            super("Groq API returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a health insurance pre-authorization assistant for a demo platform.
                Evaluate the claim strictly against the POLICY WORDING and PRE-CHECK RESULTS provided.
                Rules:
                1. Use only the provided wording. Never invent clauses, limits, or exclusions.
                2. PRE-CHECK RESULTS are facts computed by the system. Do not recompute or contradict them.
                3. APPROVE only when treatment is clearly covered and no rule blocks it.
                4. REJECT only when a clause or failed pre-check clearly blocks the claim.
                5. If wording is ambiguous, information is missing, or medical necessity is unclear, use NEEDS_HUMAN_REVIEW.
                6. Cite clause numbers relied on.
                7. Respond with one JSON object and nothing else.
                Output schema:
                {
                  "decision": "APPROVE" | "REJECT" | "NEEDS_HUMAN_REVIEW",
                  "rationale": "2-4 plain-language sentences",
                  "clauses_cited": ["Clause 3"],
                  "missing_information": [],
                  "confidence": 0.0
                }
                """;
    }

    private String buildPrompt(Claim claim, long policyAgeDays, double remaining,
                               boolean initialWaitingFailed, boolean specificWaitingFailed,
                               boolean preExistingWaitingFailed, String exclusion) {
        PolicyPlan plan = claim.getPlan();
        String cap = plan.getRoomRentCapPercent() == null ? "none" : plan.getRoomRentCapPercent() + "%";
        String details = claim.getDetails() == null || claim.getDetails().isBlank() ? "Not provided" : claim.getDetails();
        String initial = initialWaitingFailed ? "FAILED" : "PASSED";
        String specific = plan.getSpecificIllnessWaitingMonths() > 0
                ? (specificWaitingFailed ? "FAILED" : "PASSED") + " - requires " + plan.getSpecificIllnessWaitingMonths() + " months"
                : "NOT_APPLICABLE";
        String ped = Boolean.TRUE.equals(claim.getPreExistingRelated())
                ? (preExistingWaitingFailed ? "FAILED" : "PASSED") + " - declared as pre-existing; waiting is " + plan.getPedWaitingMonths() + " months"
                : "NOT_APPLICABLE";
        return """
                PLAN:
                Category: %s | Plan: %s | Sum insured: %s
                Co-pay: %s%% | Restoration: %s | Room rent cap: %s

                POLICY WORDING:
                %s

                CLAIM:
                Hospitalization type: %s | Diagnosis: %s | Treatment: %s
                Estimated cost: %s | Admission date: %s
                Policy start date: %s | Already claimed this year: %s
                Pre-existing related (declared by user): %s
                Details: %s

                PRE-CHECK RESULTS (system-computed):
                - Initial waiting (%d days): %s (policy age %d days)
                - Specific illness waiting: %s
                - Pre-existing waiting: %s
                - Remaining sum insured: %.2f, estimated cost within limit: %s
                - Exclusion keyword match: %s

                Decide using only the wording above. Respond with JSON only.
                """.formatted(plan.getCategory(), plan.getPlanName(), plan.getSumInsured(),
                plan.getCopayPercent(), plan.getRestoration(), cap, plan.getWording(),
                claim.getHospitalizationType(), claim.getDiagnosis(), claim.getProcedure(), claim.getEstimatedAmount(),
                claim.getAdmissionDate(), claim.getPolicyStartDate(), claim.getAmountClaimedThisYear(),
                claim.getPreExistingRelated(), details, plan.getInitialWaitingDays(), initial,
                policyAgeDays, specific, ped, remaining, claim.getEstimatedAmount() <= remaining ? "PASSED" : "FAILED",
                exclusion == null ? "NONE" : exclusion);
    }

    private void parseAndPersistDecision(Claim claim, String rawResponse, List<String> precheckFailures) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Strip markdown code fences if the model wraps in ```json ... ```
        content = content.strip();
            if (content.startsWith("```")) {
                content = content.replaceAll("```[a-z]*\\n?", "").strip();
                if (content.endsWith("```")) {
                    content = content.substring(0, content.lastIndexOf("```")).strip();
                }
            }

            JsonNode decision = objectMapper.readTree(content);
            String decisionValue = decision.path("decision").asText("NEEDS_HUMAN_REVIEW").toUpperCase();
            String rationale = decision.path("rationale").asText("No rationale provided");
            double confidence = decision.path("confidence").asDouble(0.5);
            List<String> clauses = new ArrayList<>();
            decision.path("clauses_cited").forEach(n -> clauses.add(n.asText()));
            List<String> missing = new ArrayList<>();
            decision.path("missing_information").forEach(n -> missing.add(n.asText()));

            if (confidence < 0.6) {
                decisionValue = "NEEDS_HUMAN_REVIEW";
                rationale = "The AI confidence was below the review threshold. " + rationale;
            }
            if (!precheckFailures.isEmpty() && !decisionValue.equals("REJECT") && !decisionValue.equals("REJECTED")) {
                decisionValue = "NEEDS_HUMAN_REVIEW";
                rationale = "A system pre-check needs review: " + String.join("; ", precheckFailures) + ". " + rationale;
            }

            // Validate decision value
            ClaimStatus newStatus = switch (decisionValue) {
                case "APPROVE", "APPROVED" -> ClaimStatus.APPROVED;
                case "REJECT", "REJECTED" -> ClaimStatus.REJECTED;
                case "NEEDS_HUMAN_REVIEW" -> ClaimStatus.NEEDS_HUMAN_REVIEW;
                default -> ClaimStatus.NEEDS_HUMAN_REVIEW;
            };

            ClaimStatus previousStatus = claim.getStatus();
            claim.setAiDecision(decisionValue);
            claim.setAiRationale(rationale);
            claim.setAiConfidence(confidence);
            claim.setClausesCited(String.join("|", clauses));
            claim.setMissingInformation(String.join("|", missing));
            claim.setStatus(newStatus);

            // Store final AI decision alongside status
            if (newStatus == ClaimStatus.APPROVED || newStatus == ClaimStatus.REJECTED) {
                claim.setAiDecision(newStatus.name());
            }

            claim.setDecidedAt(LocalDateTime.now());
            claimRepository.save(claim);

            ClaimHistory history = ClaimHistory.builder()
                    .claim(claim)
                    .fromStatus(previousStatus)
                    .toStatus(claim.getStatus())
                    .actor("AI")
                    .remarks("AI decision: " + decisionValue + " (confidence: " + String.format("%.0f%%", confidence * 100) + ") — " + rationale)
                    .timestamp(LocalDateTime.now())
                    .build();
            historyRepository.save(history);

        } catch (Exception e) {
            log.error("Failed to parse Groq response; routing claim to human review: {}", e.getMessage());
            persistFallback(claim, "AI response was invalid; routed to human review.",
                    List.of("Valid AI assessment unavailable"), 0.0, "Malformed AI response");
        }
    }

    private void persistFallback(Claim claim, String rationale, List<String> missing, double confidence, String remarks) {
        ClaimStatus previous = claim.getStatus();
        claim.setAiDecision("NEEDS_HUMAN_REVIEW");
        claim.setAiRationale(rationale);
        claim.setAiConfidence(confidence);
        claim.setMissingInformation(String.join("|", missing));
        claim.setStatus(ClaimStatus.NEEDS_HUMAN_REVIEW);
        claim.setDecidedAt(LocalDateTime.now());
        claimRepository.save(claim);
        historyRepository.save(ClaimHistory.builder().claim(claim).fromStatus(previous)
                .toStatus(ClaimStatus.NEEDS_HUMAN_REVIEW).actor("SYSTEM").remarks(remarks)
                .timestamp(LocalDateTime.now()).build());
    }
}
