package com.mediauth.repository;

import com.mediauth.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
}
