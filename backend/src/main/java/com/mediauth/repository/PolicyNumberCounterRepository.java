package com.mediauth.repository;

import com.mediauth.entity.PolicyNumberCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PolicyNumberCounterRepository extends JpaRepository<PolicyNumberCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select counter from PolicyNumberCounter counter where counter.id = :id")
    Optional<PolicyNumberCounter> findByIdForUpdate(@Param("id") String id);
}
