package com.heistplanner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "heists")
@Data
@NoArgsConstructor
public class Heist {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Target target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HeistStatus status = HeistStatus.PLANNING;

    @Enumerated(EnumType.STRING)
    private HeistResult result;

    private String resultDetails;

    // Risk score 0-100 calculated by engine
    private int riskScore = 0;

    // Success probability 0-100
    private int successProbability = 0;

    @Column(name = "payout")
    private int payout = 0; // in-game currency if successful

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id", nullable = false)
    private User planner;

    @OneToMany(mappedBy = "heist", cascade = CascadeType.ALL)
    private List<CrewMember> crew;

    @OneToMany(mappedBy = "heist", cascade = CascadeType.ALL)
    private List<HeistPhase> phases;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    public enum Target {
        CITY_BANK,        // Medium risk, medium payout
        CASINO,           // High risk, high payout
        ART_MUSEUM,       // Low risk, low payout
        FEDERAL_RESERVE,  // Very high risk, massive payout
        JEWELRY_STORE,    // Low-medium risk, medium payout
        CRYPTO_EXCHANGE   // Medium risk, high payout (digital heist)
    }

    public enum HeistStatus {
        PLANNING, CREW_ASSEMBLED, READY, EXECUTING, COMPLETED, BUSTED
    }

    public enum HeistResult {
        SUCCESS, PARTIAL_SUCCESS, FAILED, BUSTED
    }
}
