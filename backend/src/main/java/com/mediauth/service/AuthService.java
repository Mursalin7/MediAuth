package com.mediauth.service;

import com.mediauth.dto.*;
import com.mediauth.entity.*;
import com.mediauth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PatientRepository patientRepository;
    private final PolicyRepository policyRepository;
    private final PolicyNumberCounterRepository policyNumberCounterRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.mediauth.security.JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserRole role = UserRole.PATIENT;
        if ("REVIEWER".equalsIgnoreCase(request.getRole())) {
            role = UserRole.REVIEWER;
        }

        Policy policy = null;
        if (role == UserRole.PATIENT) {
            if (request.getPolicyNumber() != null && !request.getPolicyNumber().isBlank()) {
                policy = policyRepository.findByPolicyNumber(request.getPolicyNumber())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Policy not found: " + request.getPolicyNumber()));
            } else {
                PolicyNumberCounter counter = policyNumberCounterRepository.findByIdForUpdate("POL_HEALTH")
                        .orElseThrow(() -> new IllegalStateException("Policy number counter has not been initialized"));
                long nextNumber = counter.getLastIssued() + 1;
                counter.setLastIssued(nextNumber);
                policyNumberCounterRepository.save(counter);

                policy = policyRepository.save(Policy.builder()
                        .policyNumber(String.format(Locale.ROOT, "POL_HEALTH-%03d", nextNumber))
                        .coverageType(CoverageType.HEALTH)
                        .sumInsured(0.0)
                        .waitingPeriodMonths(0)
                        .exclusions(List.of())
                        .build());
            }
        }

        Patient patient = Patient.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .policy(policy)
                .role(role)
                .build();
        patientRepository.save(patient);

        String token = jwtTokenProvider.generateToken(patient.getEmail());
        return new AuthResponse(token, patient.getEmail(), patient.getRole().name(), patient.getName());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Patient patient = patientRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String token = jwtTokenProvider.generateToken(patient.getEmail());
        return new AuthResponse(token, patient.getEmail(), patient.getRole().name(), patient.getName());
    }
}
