package com.mediauth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoverageType coverageType;

    @Column(nullable = false)
    private Double sumInsured;

    @Column(nullable = false)
    private Integer waitingPeriodMonths;

    @ElementCollection
    @CollectionTable(name = "policy_exclusions", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "exclusion")
    private List<String> exclusions;
}
