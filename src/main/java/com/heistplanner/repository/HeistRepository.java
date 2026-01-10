package com.heistplanner.repository;

import com.heistplanner.entity.Heist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HeistRepository extends JpaRepository<Heist, Long> {
    List<Heist> findByPlannerId(Long plannerId);
    List<Heist> findByStatus(Heist.HeistStatus status);

    @Query("SELECT h FROM Heist h WHERE h.planner.id = :plannerId AND h.result = 'SUCCESS'")
    List<Heist> findSuccessfulHeistsByPlanner(@Param("plannerId") Long plannerId);

    @Query("SELECT h.target, COUNT(h), AVG(h.successProbability) FROM Heist h GROUP BY h.target")
    List<Object[]> findTargetStats();
}
