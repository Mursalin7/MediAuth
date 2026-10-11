package com.mediauth.controller;

import com.mediauth.dto.*;
import com.mediauth.entity.*;
import com.mediauth.repository.PatientRepository;
import com.mediauth.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final PatientRepository patientRepository;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ClaimResponse> submitClaim(
            @Valid @RequestBody ClaimSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Patient patient = resolvePatient(userDetails);
        ClaimResponse response = claimService.submitClaim(request, patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Patient patient = resolvePatient(userDetails);
        return ResponseEntity.ok(claimService.getClaim(id, patient));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<ClaimResponse> reviewClaim(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                claimService.reviewClaim(id, request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ClaimResponse> addDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Patient patient = resolvePatient(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.addDocument(id, request, patient));
    }

    @GetMapping
    public ResponseEntity<Page<ClaimResponse>> listClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        Patient patient = resolvePatient(userDetails);
        boolean isReviewer = patient.getRole() == UserRole.REVIEWER;

        ClaimStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ClaimStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter: " + status);
            }
        }

        return ResponseEntity.ok(claimService.listClaims(patient, isReviewer, statusFilter, page, size));
    }

    private Patient resolvePatient(UserDetails userDetails) {
        return patientRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
