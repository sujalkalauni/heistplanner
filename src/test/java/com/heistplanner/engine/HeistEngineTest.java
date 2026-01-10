package com.heistplanner.engine;

import com.heistplanner.entity.CrewMember;
import com.heistplanner.entity.Heist;
import com.heistplanner.entity.HeistPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeistEngineTest {

    private HeistEngine engine;

    @BeforeEach
    void setUp() { engine = new HeistEngine(); }

    @Test
    void calculate_federalReserve_highRiskWithNoCrew() {
        Heist heist = new Heist();
        heist.setTarget(Heist.Target.FEDERAL_RESERVE);

        HeistEngine.HeistCalculation calc = engine.calculate(heist, List.of(), List.of());

        assertThat(calc.riskScore()).isGreaterThanOrEqualTo(80);
        assertThat(calc.successProbability()).isLessThanOrEqualTo(20);
    }

    @Test
    void calculate_artMuseum_lowRiskWithGoodCrew() {
        Heist heist = new Heist();
        heist.setTarget(Heist.Target.ART_MUSEUM);

        CrewMember hacker = makeCrew(CrewMember.Role.HACKER, 9, 9, 10);
        CrewMember driver = makeCrew(CrewMember.Role.DRIVER, 8, 8, 10);
        CrewMember lookout = makeCrew(CrewMember.Role.LOOKOUT, 7, 9, 5);

        HeistPhase recon = makePhase(HeistPhase.Phase.RECONNAISSANCE);
        HeistPhase escape = makePhase(HeistPhase.Phase.ESCAPE_ROUTE);

        HeistEngine.HeistCalculation calc = engine.calculate(heist,
                List.of(hacker, driver, lookout), List.of(recon, escape));

        assertThat(calc.riskScore()).isLessThan(25);
        assertThat(calc.successProbability()).isGreaterThan(75);
    }

    @Test
    void calculate_lowLoyaltyCrew_increasesRisk() {
        Heist heist = new Heist();
        heist.setTarget(Heist.Target.CITY_BANK);

        CrewMember traitor = makeCrew(CrewMember.Role.MUSCLE, 8, 1, 20); // loyalty = 1

        HeistEngine.HeistCalculation calcWithTraitor = engine.calculate(heist, List.of(traitor), List.of());

        CrewMember loyal = makeCrew(CrewMember.Role.MUSCLE, 8, 9, 20); // loyalty = 9
        HeistEngine.HeistCalculation calcWithLoyal = engine.calculate(heist, List.of(loyal), List.of());

        assertThat(calcWithTraitor.riskScore()).isGreaterThan(calcWithLoyal.riskScore());
    }

    @Test
    void calculate_completedPhases_reduceRisk() {
        Heist heist = new Heist();
        heist.setTarget(Heist.Target.CASINO);

        CrewMember crew = makeCrew(CrewMember.Role.DRIVER, 5, 7, 15);

        HeistEngine.HeistCalculation noPhases = engine.calculate(heist, List.of(crew), List.of());

        HeistPhase recon = makePhase(HeistPhase.Phase.RECONNAISSANCE);
        HeistPhase entry = makePhase(HeistPhase.Phase.ENTRY_PLAN);
        HeistPhase escape = makePhase(HeistPhase.Phase.ESCAPE_ROUTE);

        HeistEngine.HeistCalculation withPhases = engine.calculate(heist, List.of(crew),
                List.of(recon, entry, escape));

        assertThat(withPhases.riskScore()).isLessThan(noPhases.riskScore());
    }

    @Test
    void calculate_crewCutsReducePayout() {
        Heist heist = new Heist();
        heist.setTarget(Heist.Target.JEWELRY_STORE);

        CrewMember greedyCrew = makeCrew(CrewMember.Role.SAFECRACKER, 8, 8, 40); // 40% cut

        HeistEngine.HeistCalculation calc = engine.calculate(heist, List.of(greedyCrew), List.of());

        // Base payout for jewelry store is 120,000 — 40% cut = 72,000
        assertThat(calc.netPayout()).isEqualTo(72_000);
    }

    // ── Helpers ──────────────────────────────────────────

    private CrewMember makeCrew(CrewMember.Role role, int skill, int loyalty, int cut) {
        CrewMember c = new CrewMember();
        c.setRole(role); c.setSkillLevel(skill);
        c.setLoyalty(loyalty); c.setCutPercentage(cut);
        c.setName("Test " + role.name());
        return c;
    }

    private HeistPhase makePhase(HeistPhase.Phase phase) {
        HeistPhase p = new HeistPhase();
        p.setPhase(phase);
        p.setStatus(HeistPhase.PhaseStatus.COMPLETED);
        return p;
    }
}
