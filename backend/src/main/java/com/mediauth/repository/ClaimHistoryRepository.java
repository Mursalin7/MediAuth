package com.mediauth.repository;

import com.mediauth.entity.ClaimHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimHistoryRepository extends JpaRepository<ClaimHistory, Long> {
    List<ClaimHistory> findByClaimIdOrderByTimestampAsc(Long claimId);
}
