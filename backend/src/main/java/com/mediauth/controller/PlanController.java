package com.mediauth.controller;

import com.mediauth.dto.PlanResponse;
import com.mediauth.repository.PolicyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PolicyPlanRepository policyPlanRepository;

    @GetMapping
    public List<PlanResponse> getPlans() {
        return policyPlanRepository.findAll().stream().map(PlanResponse::from).toList();
    }
}
