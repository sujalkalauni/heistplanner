package com.heistplanner.service;

import com.heistplanner.dto.Dtos.*;
import com.heistplanner.engine.HeistEngine;
import com.heistplanner.entity.*;
import com.heistplanner.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeistServiceTest {

    @Mock private HeistRepository heistRepository;
    @Mock private CrewMemberRepository crewRepository;
    @Mock private HeistPhaseRepository phaseRepository;
    @Mock private UserRepository userRepository;
    @Mock private HeistEngine engine;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks private HeistService heistService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("ghost");
        testUser.setReputation(50);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("ghost");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createHeist_savesAndReturns() {
        CreateHeistRequest req = new CreateHeistRequest();
        req.setName("Operation Blackout");
        req.setTarget(Heist.Target.CITY_BANK);

        Heist saved = new Heist();
        saved.setId(1L);
        saved.setName("Operation Blackout");
        saved.setTarget(Heist.Target.CITY_BANK);
        saved.setStatus(Heist.HeistStatus.PLANNING);
        saved.setPlanner(testUser);

        when(heistRepository.save(any())).thenReturn(saved);

        HeistResponse res = heistService.createHeist(req);

        assertThat(res.getName()).isEqualTo("Operation Blackout");
        assertThat(res.getTarget()).isEqualTo(Heist.Target.CITY_BANK);
        assertThat(res.getStatus()).isEqualTo(Heist.HeistStatus.PLANNING);
    }

    @Test
    void execute_throwsWhenNoCrew() {
        Heist heist = new Heist();
        heist.setId(1L);
        heist.setStatus(Heist.HeistStatus.PLANNING);
        heist.setPlanner(testUser);
        heist.setTarget(Heist.Target.CASINO);

        when(heistRepository.findById(1L)).thenReturn(Optional.of(heist));
        when(crewRepository.findByHeistId(1L)).thenReturn(List.of());
        when(phaseRepository.findByHeistId(1L)).thenReturn(List.of());
        when(engine.calculate(any(), any(), any()))
                .thenReturn(new HeistEngine.HeistCalculation(70, 30, 0));
        when(heistRepository.save(any())).thenReturn(heist);

        assertThatThrownBy(() -> heistService.execute(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("solo");
    }

    @Test
    void execute_successIncreasesReputation() {
        Heist heist = new Heist();
        heist.setId(1L);
        heist.setStatus(Heist.HeistStatus.PLANNING);
        heist.setPlanner(testUser);
        heist.setTarget(Heist.Target.ART_MUSEUM);

        CrewMember crew = new CrewMember();
        crew.setRole(CrewMember.Role.DRIVER);
        crew.setLoyalty(9);

        when(heistRepository.findById(1L)).thenReturn(Optional.of(heist));
        when(crewRepository.findByHeistId(1L)).thenReturn(List.of(crew));
        when(phaseRepository.findByHeistId(1L)).thenReturn(List.of());
        when(engine.calculate(any(), any(), any()))
                .thenReturn(new HeistEngine.HeistCalculation(20, 80, 50_000));
        when(engine.execute(anyInt(), any())).thenReturn(Heist.HeistResult.SUCCESS);
        when(heistRepository.save(any())).thenReturn(heist);
        when(userRepository.save(any())).thenReturn(testUser);

        ExecuteResponse res = heistService.execute(1L);

        assertThat(res.getResult()).isEqualTo(Heist.HeistResult.SUCCESS);
        assertThat(res.getReputationChange()).isEqualTo(10);
    }

    @Test
    void getHeist_throwsWhenNotFound() {
        when(heistRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> heistService.getHeist(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
