package com.heistplanner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crew_members")
@Data
@NoArgsConstructor
public class CrewMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Skill level 1-10
    private int skillLevel;

    // Loyalty 1-10 — low loyalty = higher chance of betrayal
    private int loyalty;

    // Cut percentage they demand from payout
    private int cutPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heist_id", nullable = false)
    private Heist heist;

    public enum Role {
        HACKER,      // Reduces alarm/tech risk
        DRIVER,      // Reduces escape risk
        MUSCLE,      // Reduces guard risk
        SAFECRACKER, // Required for bank/vault targets
        LOOKOUT,     // Reduces detection risk
        MASTERMIND   // Boosts overall success probability
    }
}
