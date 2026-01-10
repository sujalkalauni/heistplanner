package com.heistplanner.service;

import com.heistplanner.dto.Dtos.*;
import com.heistplanner.engine.HeistEngine;
import com.heistplanner.entity.*;
import com.heistplanner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeistService {

    private final HeistRepository heistRepository;
    private final CrewMemberRepository crewRepository;
    private final HeistPhaseRepository phaseRepository;
    private final UserRepository userRepository;
    private final HeistEngine engine;

    public HeistResponse createHeist(CreateHeistRequest req) {
        User user = currentUser();
        Heist heist = new Heist();
        heist.setName(req.getName());
        heist.setTarget(req.getTarget());
        heist.setPlanner(user);
        return HeistResponse.from(heistRepository.save(heist));
    }

    public HeistResponse getHeist(Long id) {
        return HeistResponse.from(findHeist(id));
    }

    public List<HeistResponse> myHeists() {
        return heistRepository.findByPlannerId(currentUser().getId())
                .stream().map(HeistResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public CrewMemberResponse addCrew(Long heistId, AddCrewRequest req) {
        Heist heist = findHeist(heistId);
        assertPlanning(heist);

        CrewMember member = new CrewMember();
        member.setHeist(heist);
        member.setName(req.getName());
        member.setRole(req.getRole());
        member.setSkillLevel(req.getSkillLevel());
        member.setLoyalty(req.getLoyalty());
        member.setCutPercentage(req.getCutPercentage());

        CrewMember saved = crewRepository.save(member);
        recalculate(heist);
        return CrewMemberResponse.from(saved);
    }

    @Transactional
    public PhaseResponse addPhase(Long heistId, AddPhaseRequest req) {
        Heist heist = findHeist(heistId);
        assertPlanning(heist);

        HeistPhase phase = new HeistPhase();
        phase.setHeist(heist);
        phase.setPhase(req.getPhase());
        phase.setNotes(req.getNotes());
        phase.setStatus(HeistPhase.PhaseStatus.COMPLETED);
        phase.setRiskContribution(phaseRiskContribution(req.getPhase()));

        HeistPhase saved = phaseRepository.save(phase);
        recalculate(heist);
        return PhaseResponse.from(saved);
    }

    @Transactional
    public ExecuteResponse execute(Long heistId) {
        Heist heist = findHeist(heistId);

        if (heist.getStatus() == Heist.HeistStatus.BUSTED ||
            heist.getStatus() == Heist.HeistStatus.COMPLETED) {
            throw new IllegalStateException("This heist is already done.");
        }

        List<CrewMember> crew = crewRepository.findByHeistId(heistId);
        if (crew.isEmpty()) throw new IllegalStateException("You can't run a heist solo. Recruit some crew.");

        recalculate(heist);

        Heist.HeistResult result = engine.execute(heist.getSuccessProbability(), crew);
        heist.setResult(result);
        heist.setExecutedAt(LocalDateTime.now());

        ExecuteResponse response = new ExecuteResponse();
        response.setResult(result);
        response.setNewSuccessProbability(heist.getSuccessProbability());

        int reputationChange = 0;
        switch (result) {
            case SUCCESS -> {
                heist.setStatus(Heist.HeistStatus.COMPLETED);
                heist.setResultDetails("Clean getaway. No witnesses. The crew splits " + heist.getPayout() + " credits.");
                response.setPayout(heist.getPayout());
                response.setNarrative("Everything went according to plan. You're a legend.");
                reputationChange = 10;
            }
            case PARTIAL_SUCCESS -> {
                heist.setStatus(Heist.HeistStatus.COMPLETED);
                int partial = heist.getPayout() / 2;
                heist.setPayout(partial);
                heist.setResultDetails("Got out with half the take. Someone tripped the alarm.");
                response.setPayout(partial);
                response.setNarrative("Not perfect, but you're alive and richer. Take the win.");
                reputationChange = 3;
            }
            case FAILED -> {
                heist.setStatus(Heist.HeistStatus.COMPLETED);
                heist.setPayout(0);
                heist.setResultDetails("Abort abort abort. Got nothing, but no one's in cuffs.");
                response.setPayout(0);
                response.setNarrative("You walked away empty-handed. Live to plan another day.");
                reputationChange = -2;
            }
            case BUSTED -> {
                heist.setStatus(Heist.HeistStatus.BUSTED);
                heist.setPayout(0);
                heist.setResultDetails("Someone talked. The whole crew got pinched.");
                response.setPayout(0);
                response.setNarrative("Lights. Sirens. Handcuffs. Someone on your crew had loose lips.");
                reputationChange = -10;
            }
        }

        response.setReputationChange(reputationChange);
        heistRepository.save(heist);

        // Update planner reputation
        User planner = heist.getPlanner();
        planner.setReputation(planner.getReputation() + reputationChange);
        userRepository.save(planner);

        return response;
    }

    public PlannerStatsResponse myStats() {
        User user = currentUser();
        List<Heist> heists = heistRepository.findByPlannerId(user.getId());

        PlannerStatsResponse stats = new PlannerStatsResponse();
        stats.setUsername(user.getUsername());
        stats.setReputation(user.getReputation());
        stats.setTotalHeists(heists.size());
        stats.setSuccessfulHeists((int) heists.stream()
                .filter(h -> h.getResult() == Heist.HeistResult.SUCCESS).count());
        stats.setBustedCount((int) heists.stream()
                .filter(h -> h.getResult() == Heist.HeistResult.BUSTED).count());
        stats.setTotalEarnings(heists.stream().mapToInt(Heist::getPayout).sum());
        stats.setSuccessRate(stats.getTotalHeists() > 0
                ? (double) stats.getSuccessfulHeists() / stats.getTotalHeists() * 100 : 0);
        return stats;
    }

    public List<TargetStatsResponse> targetStats() {
        return heistRepository.findTargetStats().stream().map(row -> {
            TargetStatsResponse r = new TargetStatsResponse();
            r.setTarget((Heist.Target) row[0]);
            r.setTotalAttempts((Long) row[1]);
            r.setAvgSuccessProbability(((Number) row[2]).doubleValue());
            return r;
        }).collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────

    private void recalculate(Heist heist) {
        List<CrewMember> crew = crewRepository.findByHeistId(heist.getId());
        List<HeistPhase> phases = phaseRepository.findByHeistId(heist.getId());
        HeistEngine.HeistCalculation calc = engine.calculate(heist, crew, phases);
        heist.setRiskScore(calc.riskScore());
        heist.setSuccessProbability(calc.successProbability());
        heist.setPayout(calc.netPayout());
        heist.setStatus(crew.isEmpty() ? Heist.HeistStatus.PLANNING : Heist.HeistStatus.CREW_ASSEMBLED);
        heistRepository.save(heist);
    }

    private Heist findHeist(Long id) {
        return heistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Heist not found: " + id));
    }

    private void assertPlanning(Heist heist) {
        if (heist.getStatus() == Heist.HeistStatus.COMPLETED ||
            heist.getStatus() == Heist.HeistStatus.BUSTED) {
            throw new IllegalStateException("Can't modify a heist that's already done.");
        }
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private int phaseRiskContribution(HeistPhase.Phase phase) {
        return switch (phase) {
            case RECONNAISSANCE -> 20;
            case EQUIPMENT -> 15;
            case ENTRY_PLAN -> 25;
            case ESCAPE_ROUTE -> 20;
            case CONTINGENCY -> 10;
        };
    }
}
