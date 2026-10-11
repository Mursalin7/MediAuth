package com.mediauth.config;

import com.mediauth.entity.*;
import com.mediauth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.math.BigDecimal;

/**
 * Seeds demo data on first startup if the DB is empty.
 * Safe to run on Neon (only creates records if none exist).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    CommandLineRunner seed(PatientRepository patientRepo,
                           PolicyRepository policyRepo,
                           PolicyPlanRepository policyPlanRepo,
                           PolicyNumberCounterRepository policyNumberCounterRepo,
                           PasswordEncoder encoder) {
        return args -> {
            seedPolicyPlans(policyPlanRepo);
            policyNumberCounterRepo.findById("POL_HEALTH")
                    .orElseGet(() -> policyNumberCounterRepo.save(new PolicyNumberCounter("POL_HEALTH", 0)));
            if (patientRepo.count() > 0) {
                log.info("Policy plan reference data ready ({} plans)", policyPlanRepo.count());
                return;
            }

            log.info("Seeding demo data...");

            Policy policy1 = policyRepo.save(Policy.builder()
                    .policyNumber("POL-HEALTH-001")
                    .coverageType(CoverageType.HEALTH)
                    .sumInsured(500000.0)
                    .waitingPeriodMonths(3)
                    .exclusions(List.of("Cosmetic surgery", "Dental treatment", "Pre-existing conditions"))
                    .build());

            Policy policy2 = policyRepo.save(Policy.builder()
                    .policyNumber("POL-HOSP-002")
                    .coverageType(CoverageType.HOSPITALIZATION)
                    .sumInsured(300000.0)
                    .waitingPeriodMonths(6)
                    .exclusions(List.of("Outpatient", "Physiotherapy"))
                    .build());

            patientRepo.save(Patient.builder()
                    .name("Alice Johnson")
                    .email("alice@example.com")
                    .password(encoder.encode("password123"))
                    .policy(policy1)
                    .role(UserRole.PATIENT)
                    .build());

            patientRepo.save(Patient.builder()
                    .name("Bob Smith")
                    .email("bob@example.com")
                    .password(encoder.encode("password123"))
                    .policy(policy2)
                    .role(UserRole.PATIENT)
                    .build());

            patientRepo.save(Patient.builder()
                    .name("Dr. Reviewer")
                    .email("reviewer@example.com")
                    .password(encoder.encode("reviewer123"))
                    .policy(null)
                    .role(UserRole.REVIEWER)
                    .build());

            log.info("Demo data seeded. Patients: alice@example.com, bob@example.com | Reviewer: reviewer@example.com");
            log.info("Policy plan reference data ready ({} plans)", policyPlanRepo.count());
        };
    }

    private void seedPolicyPlans(PolicyPlanRepository repository) {
        List<PolicyPlan> plans = List.of(
                plan("INDIVIDUAL", "Basic Individual Plan", 300000, 30, 24, 36, 0, 1.00, "NONE",
                        "cosmetic surgery,dental (non-accident),maternity,infertility,OPD,obesity surgery",
                        """
                        BASIC INDIVIDUAL HEALTH PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury. This includes room, nursing, doctor fees, surgery, medicines and diagnostics during the stay.
                        Clause 2 - Sum Insured: Rs 3,00,000 per policy year for the insured person. No claim can exceed the remaining sum insured.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones, joint replacement, hysterectomy and tonsillectomy are covered only after 24 months. Pre-existing diseases are covered only after 36 months of continuous coverage.
                        Clause 4 - Room Rent: Room rent is capped at 1% of sum insured per day (Rs 3,000) and ICU at 2% (Rs 6,000) per day. If a higher room is chosen, related expenses are reduced proportionately.
                        Clause 5 - Co-payment: None.
                        Clause 6 - Restoration: Not available.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 30 days before admission and 60 days after discharge are covered within the sum insured.
                        Clause 8 - Day Care: Listed day-care procedures requiring less than 24 hours of hospitalization are covered.
                        Clause 9 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), maternity and infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, experimental or unproven treatment, and non-medical consumables.
                        Clause 10 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """),
                plan("INDIVIDUAL", "Comprehensive Individual Plan", 1000000, 30, 12, 24, 0, null, "ONCE",
                        "cosmetic surgery,dental (non-accident),maternity,infertility,OPD,obesity surgery",
                        """
                        COMPREHENSIVE INDIVIDUAL HEALTH PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury, including room, nursing, doctor fees, surgery, implants, medicines, diagnostics, robotic and advanced surgical procedures, and ambulance charges up to Rs 3,000 per hospitalization.
                        Clause 2 - Sum Insured: Rs 10,00,000 per policy year for the insured person. No claim can exceed the remaining sum insured.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones and joint replacement are covered only after 12 months. Pre-existing diseases are covered only after 24 months of continuous coverage.
                        Clause 4 - Room Rent: No cap on room rent. Single private room or lower category is covered without proportionate deduction.
                        Clause 5 - Co-payment: None.
                        Clause 6 - Restoration: If the sum insured is exhausted during the policy year, it is restored once for an unrelated illness.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 60 days before admission and 90 days after discharge are covered.
                        Clause 8 - Day Care: All day-care procedures requiring less than 24 hours of hospitalization due to technological advancement are covered.
                        Clause 9 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), maternity and infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, and experimental or unproven treatment.
                        Clause 10 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """),
                plan("FAMILY_FLOATER", "Basic Family Floater Plan", 500000, 30, 24, 36, 0, 1.00, "NONE",
                        "cosmetic surgery,dental (non-accident),maternity,infertility,OPD,obesity surgery",
                        """
                        BASIC FAMILY FLOATER PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury for self, spouse and up to 2 dependent children. This includes room, nursing, doctor fees, surgery, medicines and diagnostics during the stay.
                        Clause 2 - Sum Insured: Rs 5,00,000 per policy year shared by all insured members. Any one member may use the entire sum insured and claims by all members reduce the same balance.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones, joint replacement and hysterectomy are covered only after 24 months. Pre-existing diseases are covered only after 36 months of continuous coverage.
                        Clause 4 - Room Rent: Room rent is capped at 1% of sum insured per day (Rs 5,000) and ICU at 2% (Rs 10,000) per day. If a higher room is chosen, related expenses are reduced proportionately.
                        Clause 5 - Co-payment: None.
                        Clause 6 - Restoration: Not available. Once the shared sum insured is exhausted, no further claims are payable in that policy year.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 30 days before admission and 60 days after discharge are covered within the sum insured.
                        Clause 8 - Day Care: Listed day-care procedures requiring less than 24 hours of hospitalization are covered.
                        Clause 9 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), maternity and infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, experimental or unproven treatment, and non-medical consumables.
                        Clause 10 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """),
                plan("FAMILY_FLOATER", "Comprehensive Family Floater Plan", 1500000, 30, 12, 24, 0, null, "UNLIMITED",
                        "cosmetic surgery,dental (non-accident),infertility,OPD,obesity surgery",
                        """
                        COMPREHENSIVE FAMILY FLOATER PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury for self, spouse and up to 3 dependent children, including room, nursing, doctor fees, surgery, implants, medicines, diagnostics, robotic and advanced procedures, and ambulance charges up to Rs 3,000 per hospitalization.
                        Clause 2 - Sum Insured: Rs 15,00,000 per policy year shared by all insured members. Any one member may use the entire sum insured.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones and joint replacement are covered only after 12 months. Pre-existing diseases are covered only after 24 months of continuous coverage.
                        Clause 4 - Room Rent: No cap on room rent. Single private room or lower category is covered without proportionate deduction.
                        Clause 5 - Co-payment: None.
                        Clause 6 - Restoration: If the shared sum insured is exhausted or reduced during the policy year, it is restored in full any number of times for any member and any illness.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 60 days before admission and 90 days after discharge are covered.
                        Clause 8 - Day Care: All day-care procedures requiring less than 24 hours of hospitalization due to technological advancement are covered.
                        Clause 9 - Maternity: Maternity expenses are covered after 24 months of continuous coverage up to Rs 50,000 for normal delivery and Rs 75,000 for caesarean delivery. A newborn is covered for the first 90 days.
                        Clause 10 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, and experimental or unproven treatment.
                        Clause 11 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """),
                plan("SENIOR", "Basic Senior Citizen Plan", 500000, 30, 24, 36, 20, 1.00, "NONE",
                        "cosmetic surgery,dental (non-accident),maternity,infertility,OPD,obesity surgery",
                        """
                        BASIC SENIOR CITIZEN PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury for insured persons aged 60 to 75 at entry. This includes room, nursing, doctor fees, surgery, medicines and diagnostics during the stay.
                        Clause 2 - Sum Insured: Rs 5,00,000 per policy year for the insured person. No claim can exceed the remaining sum insured.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones and joint replacement are covered only after 24 months. Pre-existing diseases are covered only after 36 months of continuous coverage.
                        Clause 4 - Room Rent: Room rent is capped at 1% of sum insured per day (Rs 5,000) and ICU at 2% (Rs 10,000) per day. If a higher room is chosen, related expenses are reduced proportionately.
                        Clause 5 - Co-payment: The insured bears 20% of every admissible claim amount. The insurer pays the remaining 80% up to the sum insured.
                        Clause 6 - Restoration: Not available.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 30 days before admission and 60 days after discharge are covered within the sum insured.
                        Clause 8 - Day Care: Listed day-care procedures requiring less than 24 hours of hospitalization are covered.
                        Clause 9 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), maternity and infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, experimental or unproven treatment, and non-medical consumables.
                        Clause 10 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """),
                plan("SENIOR", "Comprehensive Senior Citizen Plan", 1500000, 30, 12, 12, 10, null, "ONCE",
                        "cosmetic surgery,dental (non-accident),maternity,infertility,OPD,obesity surgery",
                        """
                        COMPREHENSIVE SENIOR CITIZEN PLAN - POLICY WORDING (ILLUSTRATIVE)
                        Clause 1 - Coverage: Covers reasonable and customary expenses of in-patient hospitalization of 24 hours or more due to illness or accidental injury for insured persons aged 60 to 75 at entry, including room, nursing, doctor fees, surgery, implants, medicines, diagnostics, robotic and advanced procedures, and ambulance charges up to Rs 3,000 per hospitalization.
                        Clause 2 - Sum Insured: Rs 15,00,000 per policy year for the insured person. No claim can exceed the remaining sum insured.
                        Clause 3 - Waiting Periods: An initial waiting period of 30 days from policy start applies to illness claims (accidents are exempt). Specific illnesses such as cataract, hernia, piles, kidney stones and joint replacement are covered only after 12 months. Pre-existing diseases are covered only after 12 months of continuous coverage.
                        Clause 4 - Room Rent: No cap on room rent. Single private room or lower category is covered without proportionate deduction.
                        Clause 5 - Co-payment: The insured bears 10% of every admissible claim amount. The insurer pays the remaining 90% up to the sum insured.
                        Clause 6 - Restoration: If the sum insured is exhausted during the policy year, it is restored once for an unrelated illness.
                        Clause 7 - Pre and Post Hospitalization: Relevant expenses up to 60 days before admission and 90 days after discharge are covered.
                        Clause 8 - Day Care: All day-care procedures requiring less than 24 hours of hospitalization due to technological advancement are covered.
                        Clause 9 - Health Check-up: One free preventive health check-up per policy year, not deducted from the sum insured.
                        Clause 10 - Exclusions: Cosmetic or plastic surgery (unless reconstructive after an accident or disease), dental treatment (unless due to accident), maternity and infertility treatment, OPD consultations, obesity or weight-loss surgery, self-inflicted injury, substance abuse treatment, war, and experimental or unproven treatment.
                        Clause 11 - Pre-Authorization: Planned hospitalization requires pre-authorization with diagnosis, treatment plan and estimated cost. The insurer may approve, reject or raise a query. Emergency admissions must be intimated within 24 hours.
                        """)
        );

        for (PolicyPlan plan : plans) {
            if (!repository.existsByCategoryAndPlanName(plan.getCategory(), plan.getPlanName())) {
                repository.save(plan);
            }
        }
    }

    private PolicyPlan plan(String category, String name, long sumInsured,
                            int initialWaitingDays, int specificIllnessWaitingMonths,
                            int pedWaitingMonths, int copayPercent, Double roomRentCapPercent,
                            String restoration, String exclusions, String wording) {
        return PolicyPlan.builder()
                .category(category)
                .planName(name)
                .sumInsured(BigDecimal.valueOf(sumInsured))
                .initialWaitingDays(initialWaitingDays)
                .specificIllnessWaitingMonths(specificIllnessWaitingMonths)
                .pedWaitingMonths(pedWaitingMonths)
                .copayPercent(copayPercent)
                .roomRentCapPercent(roomRentCapPercent == null ? null : BigDecimal.valueOf(roomRentCapPercent))
                .restoration(restoration)
                .exclusions(exclusions)
                .wording(wording.strip())
                .build();
    }
}
