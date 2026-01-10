package com.heistplanner.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "heist_phases")
@Data
@NoArgsConstructor
public class HeistPhase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heist_id", nullable = false)
    private Heist heist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status = PhaseStatus.PENDING;

    private String notes;

    // Risk contribution of this phase (0-100)
    private int riskContribution;

    public enum Phase {
        RECONNAISSANCE,  // Scout the target
        EQUIPMENT,       // Acquire tools/weapons
        ENTRY_PLAN,      // Plan how to get in
        ESCAPE_ROUTE,    // Plan how to get out
        CONTINGENCY      // What if things go wrong
    }

    public enum PhaseStatus {
        PENDING, COMPLETED, SKIPPED
    }
}
