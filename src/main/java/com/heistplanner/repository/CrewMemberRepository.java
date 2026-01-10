package com.heistplanner.repository;

import com.heistplanner.entity.CrewMember;
import com.heistplanner.entity.HeistPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {
    List<CrewMember> findByHeistId(Long heistId);
    void deleteByHeistId(Long heistId);
}
