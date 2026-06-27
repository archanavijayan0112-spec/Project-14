package com.floodguard.repository;

import com.floodguard.entity.Incident;
import com.floodguard.enums.IncidentStatus;
import com.floodguard.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    List<Incident> findBySeverityAndStatusOrderByCreatedAtDesc(Severity severity, IncidentStatus status);

    List<Incident> findByIsSosTrueAndStatusNotOrderByCreatedAtDesc(IncidentStatus status);

    List<Incident> findByZoneNameOrderBySeverityDescCreatedAtDesc(String zoneName);

    long countByStatus(IncidentStatus status);

    long countBySeverityAndStatus(Severity severity, IncidentStatus status);

    long countByIsSosTrueAndStatusNot(IncidentStatus status);

    /**
     * Find incidents within a given radius (in km) using Haversine formula.
     */
    @Query(value = """
        SELECT * FROM incidents i
        WHERE (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(i.latitude)) *
                cos(radians(i.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(i.latitude))
            )
        ) <= :radiusKm
        AND i.status != 'CLOSED'
        ORDER BY i.severity, i.created_at DESC
        """, nativeQuery = true)
    List<Incident> findIncidentsWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Find unassigned SOS incidents for dispatch.
     */
    @Query("SELECT i FROM Incident i WHERE i.isSos = true AND i.assignedVolunteer IS NULL AND i.status = 'OPEN' ORDER BY i.severity DESC, i.createdAt ASC")
    List<Incident> findUnassignedSosIncidents();

    List<Incident> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
