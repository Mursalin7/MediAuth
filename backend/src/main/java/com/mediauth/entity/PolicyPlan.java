package com.mediauth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "policy_plan", uniqueConstraints = @UniqueConstraint(
        name = "uk_policy_plan_category_name",
        columnNames = {"category", "plan_name"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "sum_insured", nullable = false, precision = 12, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "initial_waiting_days", nullable = false)
    private Integer initialWaitingDays;

    @Column(name = "specific_illness_waiting_months", nullable = false)
    private Integer specificIllnessWaitingMonths;

    @Column(name = "ped_waiting_months", nullable = false)
    private Integer pedWaitingMonths;

    @Column(name = "copay_percent", nullable = false)
    private Integer copayPercent;

    @Column(name = "room_rent_cap_percent", precision = 4, scale = 2)
    private BigDecimal roomRentCapPercent;

    @Column(nullable = false, length = 10)
    private String restoration;

    @Column(nullable = false, columnDefinition = "text")
    private String exclusions;

    @Column(nullable = false, columnDefinition = "text")
    private String wording;
}
