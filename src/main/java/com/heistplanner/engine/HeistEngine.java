package com.heistplanner.engine;

import com.heistplanner.entity.CrewMember;
import com.heistplanner.entity.Heist;
import com.heistplanner.entity.HeistPhase;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Core probabilistic engine for heist outcome calculation.
 * Each target has a base risk. Crew roles reduce specific risk categories.
 * Completed planning phases reduce overall risk.
 * Final outcome is determined by weighted random roll against success probability.
 */
@Component
public class HeistEngine {

    private static final Random RANDOM = new Random();

    // Base risk per target (0-100, higher = riskier)
    private static final Map<Heist.Target, Integer> BASE_RISK = Map.of(
            Heist.Target.ART_MUSEUM,       25,
            Heist.Target.JEWELRY_STORE,    35,
            Heist.Target.CITY_BANK,        55,
            Heist.Target.CRYPTO_EXCHANGE,  50,
            Heist.Target.CASINO,           70,
            Heist.Target.FEDERAL_RESERVE,  90
    );

    // Base payout per target
    private static final Map<Heist.Target, Integer> BASE_PAYOUT = Map.of(
            Heist.Target.ART_MUSEUM,       50_000,
            Heist.Target.JEWELRY_STORE,    120_000,
            Heist.Target.CITY_BANK,        500_000,
            Heist.Target.CRYPTO_EXCHANGE,  750_000,
            Heist.Target.CASINO,           1_000_000,
            Heist.Target.FEDERAL_RESERVE,  5_000_000
    );

    /**
     * Calculate risk score and success probability based on crew and planning phases.
     */
    public HeistCalculation calculate(Heist heist, List<CrewMember> crew, List<HeistPhase> phases) {
        int baseRisk = BASE_RISK.getOrDefault(heist.getTarget(), 50);
        int riskReduction = 0;

        // Each crew role reduces specific risk
        Map<CrewMember.Role, Long> roleCount = crew.stream()
                .collect(Collectors.groupingBy(CrewMember::getRole, Collectors.counting()));

        riskReduction += crewRiskReduction(roleCount, crew);

        // Each completed phase reduces risk
        long completedPhases = phases.stream()
                .filter(p -> p.getStatus() == HeistPhase.PhaseStatus.COMPLETED)
                .count();
        riskReduction += (int)(completedPhases * 6); // 6 points per completed phase

        // Loyalty penalty — low loyalty crew increases risk
        int loyaltyPenalty = crew.stream()
                .filter(c -> c.getLoyalty() < 5)
                .mapToInt(c -> (5 - c.getLoyalty()) * 2)
                .sum();

        int finalRisk = Math.min(100, Math.max(0, baseRisk - riskReduction + loyaltyPenalty));
        int successProbability = 100 - finalRisk;

        // Payout reduced by crew cuts
        int totalCutPercent = crew.stream().mapToInt(CrewMember::getCutPercentage).sum();
        int basePayout = BASE_PAYOUT.getOrDefault(heist.getTarget(), 100_000);
        int netPayout = (int)(basePayout * (1.0 - totalCutPercent / 100.0));

        return new HeistCalculation(finalRisk, successProbability, netPayout);
    }

    /**
     * Execute the heist — probabilistic outcome roll.
     */
    public Heist.HeistResult execute(int successProbability, List<CrewMember> crew) {
        int roll = RANDOM.nextInt(100);

        // Check for betrayal first — low loyalty crew member might tip off police
        boolean betrayed = crew.stream()
                .anyMatch(c -> c.getLoyalty() <= 2 && RANDOM.nextInt(10) < 3);
        if (betrayed) {
            return Heist.HeistResult.BUSTED;
        }

        if (roll < successProbability) {
            // Full success or partial based on margin
            return (roll < successProbability - 15)
                    ? Heist.HeistResult.SUCCESS
                    : Heist.HeistResult.PARTIAL_SUCCESS;
        } else if (roll < successProbability + 20) {
            return Heist.HeistResult.FAILED;
        } else {
            return Heist.HeistResult.BUSTED;
        }
    }

    private int crewRiskReduction(Map<CrewMember.Role, Long> roleCount, List<CrewMember> crew) {
        int reduction = 0;

        // Hacker reduces tech/alarm risk
        if (roleCount.containsKey(CrewMember.Role.HACKER)) {
            int avgSkill = avgSkill(crew, CrewMember.Role.HACKER);
            reduction += avgSkill * 2;
        }
        // Driver reduces escape risk
        if (roleCount.containsKey(CrewMember.Role.DRIVER)) {
            reduction += avgSkill(crew, CrewMember.Role.DRIVER);
        }
        // Muscle reduces guard confrontation risk
        if (roleCount.containsKey(CrewMember.Role.MUSCLE)) {
            reduction += avgSkill(crew, CrewMember.Role.MUSCLE);
        }
        // Safecracker is required for bank/vault — huge reduction
        if (roleCount.containsKey(CrewMember.Role.SAFECRACKER)) {
            reduction += avgSkill(crew, CrewMember.Role.SAFECRACKER) * 3;
        }
        // Lookout reduces detection
        if (roleCount.containsKey(CrewMember.Role.LOOKOUT)) {
            reduction += avgSkill(crew, CrewMember.Role.LOOKOUT);
        }
        // Mastermind boosts everything
        if (roleCount.containsKey(CrewMember.Role.MASTERMIND)) {
            reduction += avgSkill(crew, CrewMember.Role.MASTERMIND) * 2;
        }

        return Math.min(reduction, 60); // cap at 60 reduction
    }

    private int avgSkill(List<CrewMember> crew, CrewMember.Role role) {
        return (int) crew.stream()
                .filter(c -> c.getRole() == role)
                .mapToInt(CrewMember::getSkillLevel)
                .average()
                .orElse(0);
    }

    public record HeistCalculation(int riskScore, int successProbability, int netPayout) {}
}
