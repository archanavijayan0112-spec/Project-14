package com.floodguard.repository;

import com.floodguard.entity.FloodZone;
import com.floodguard.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FloodZoneRepository extends JpaRepository<FloodZone, Long> {
    Optional<FloodZone> findByZoneName(String zoneName);
    List<FloodZone> findBySeverity(Severity severity);
    List<FloodZone> findByEvacuationRequiredTrue();
    long countBySeverityIn(List<Severity> severities);
}
