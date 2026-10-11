package com.mediauth.repository;

import com.mediauth.entity.PolicyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyPlanRepository extends JpaRepository<PolicyPlan, Long> {
    boolean existsByCategoryAndPlanName(String category, String planName);
}
