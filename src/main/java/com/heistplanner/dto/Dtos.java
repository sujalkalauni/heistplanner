package com.heistplanner.dto;

import com.heistplanner.entity.CrewMember;
import com.heistplanner.entity.Heist;
import com.heistplanner.entity.HeistPhase;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // ── Auth ──────────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank private String username;
        @NotBlank @Email private String email;
        @NotBlank private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private int reputation;
        public AuthResponse(String token, String username, int reputation) {
            this.token = token; this.username = username; this.reputation = reputation;
        }
    }

    // ── Heist ─────────────────────────────────────────────

    @Data
    public static class CreateHeistRequest {
        @NotBlank private String name;
        @NotNull private Heist.Target target;
    }

    @Data
    public static class HeistResponse {
        private Long id;
        private String name;
        private Heist.Target target;
        private Heist.HeistStatus status;
        private Heist.HeistResult result;
        private String resultDetails;
        private int riskScore;
        private int successProbability;
        private int payout;
        private String plannerUsername;
        private LocalDateTime createdAt;
        private LocalDateTime executedAt;
        private List<CrewMemberResponse> crew;

        public static HeistResponse from(Heist h) {
            HeistResponse r = new HeistResponse();
            r.id = h.getId();
            r.name = h.getName();
            r.target = h.getTarget();
            r.status = h.getStatus();
            r.result = h.getResult();
            r.resultDetails = h.getResultDetails();
            r.riskScore = h.getRiskScore();
            r.successProbability = h.getSuccessProbability();
            r.payout = h.getPayout();
            r.plannerUsername = h.getPlanner() != null ? h.getPlanner().getUsername() : null;
            r.createdAt = h.getCreatedAt();
            r.executedAt = h.getExecutedAt();
            return r;
        }
    }

    // ── Crew ──────────────────────────────────────────────

    @Data
    public static class AddCrewRequest {
        @NotBlank private String name;
        @NotNull private CrewMember.Role role;
        @Min(1) @Max(10) private int skillLevel;
        @Min(1) @Max(10) private int loyalty;
        @Min(1) @Max(50) private int cutPercentage;
    }

    @Data
    public static class CrewMemberResponse {
        private Long id;
        private String name;
        private CrewMember.Role role;
        private int skillLevel;
        private int loyalty;
        private int cutPercentage;

        public static CrewMemberResponse from(CrewMember c) {
            CrewMemberResponse r = new CrewMemberResponse();
            r.id = c.getId(); r.name = c.getName(); r.role = c.getRole();
            r.skillLevel = c.getSkillLevel(); r.loyalty = c.getLoyalty();
            r.cutPercentage = c.getCutPercentage();
            return r;
        }
    }

    // ── Phase ─────────────────────────────────────────────

    @Data
    public static class AddPhaseRequest {
        @NotNull private HeistPhase.Phase phase;
        private String notes;
    }

    @Data
    public static class PhaseResponse {
        private Long id;
        private HeistPhase.Phase phase;
        private HeistPhase.PhaseStatus status;
        private String notes;
        private int riskContribution;

        public static PhaseResponse from(HeistPhase p) {
            PhaseResponse r = new PhaseResponse();
            r.id = p.getId(); r.phase = p.getPhase(); r.status = p.getStatus();
            r.notes = p.getNotes(); r.riskContribution = p.getRiskContribution();
            return r;
        }
    }

    // ── Stats ─────────────────────────────────────────────

    @Data
    public static class PlannerStatsResponse {
        private String username;
        private int reputation;
        private int totalHeists;
        private int successfulHeists;
        private int bustedCount;
        private int totalEarnings;
        private double successRate;
    }

    @Data
    public static class TargetStatsResponse {
        private Heist.Target target;
        private long totalAttempts;
        private double avgSuccessProbability;
    }

    // ── Execute result ────────────────────────────────────

    @Data
    public static class ExecuteResponse {
        private Heist.HeistResult result;
        private String narrative;
        private int payout;
        private int reputationChange;
        private int newSuccessProbability;
    }
}
