package com.heistplanner.repository;

import com.heistplanner.entity.HeistPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HeistPhaseRepository extends JpaRepository<HeistPhase, Long> {
    List<HeistPhase> findByHeistId(Long heistId);
}
