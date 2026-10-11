package com.mediauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policy_number_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyNumberCounter {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "last_issued", nullable = false)
    private long lastIssued;
}
