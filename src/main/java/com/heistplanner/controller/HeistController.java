package com.heistplanner.controller;

import com.heistplanner.dto.Dtos.*;
import com.heistplanner.service.HeistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/heists")
@RequiredArgsConstructor
@Tag(name = "Heists", description = "Plan, crew up, and execute heists")
public class HeistController {

    private final HeistService heistService;

    @PostMapping
    @Operation(summary = "Create a new heist")
    public ResponseEntity<HeistResponse> create(@Valid @RequestBody CreateHeistRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(heistService.createHeist(req));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get heist details")
    public ResponseEntity<HeistResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(heistService.getHeist(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "List all your heists")
    public ResponseEntity<List<HeistResponse>> mine() {
        return ResponseEntity.ok(heistService.myHeists());
    }

    @PostMapping("/{id}/crew")
    @Operation(summary = "Recruit a crew member")
    public ResponseEntity<CrewMemberResponse> addCrew(@PathVariable Long id,
                                                       @Valid @RequestBody AddCrewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(heistService.addCrew(id, req));
    }

    @PostMapping("/{id}/phases")
    @Operation(summary = "Complete a planning phase")
    public ResponseEntity<PhaseResponse> addPhase(@PathVariable Long id,
                                                   @Valid @RequestBody AddPhaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(heistService.addPhase(id, req));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Execute the heist — probabilistic outcome",
               description = "Rolls against your success probability. Crew loyalty, skills, and planning phases all affect the result.")
    public ResponseEntity<ExecuteResponse> execute(@PathVariable Long id) {
        return ResponseEntity.ok(heistService.execute(id));
    }

    @GetMapping("/stats/me")
    @Operation(summary = "Your planner stats — reputation, earnings, success rate")
    public ResponseEntity<PlannerStatsResponse> myStats() {
        return ResponseEntity.ok(heistService.myStats());
    }

    @GetMapping("/stats/targets")
    @Operation(summary = "Global target difficulty stats")
    public ResponseEntity<List<TargetStatsResponse>> targetStats() {
        return ResponseEntity.ok(heistService.targetStats());
    }
}
