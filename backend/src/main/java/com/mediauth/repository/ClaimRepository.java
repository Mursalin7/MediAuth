package com.mediauth.repository;

import com.mediauth.entity.Claim;
import com.mediauth.entity.ClaimStatus;
import com.mediauth.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Page<Claim> findByPatient(Patient patient, Pageable pageable);
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
    Page<Claim> findByPatientAndStatus(Patient patient, ClaimStatus status, Pageable pageable);
    Optional<Claim> findByIdAndPatient(Long id, Patient patient);
}
