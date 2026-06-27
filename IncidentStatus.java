package com.floodguard.repository;

import com.floodguard.entity.Volunteer;
import com.floodguard.enums.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    List<Volunteer> findByStatus(VolunteerStatus status);

    List<Volunteer> findByAssignedZone(String zone);

    Optional<Volunteer> findByPhone(String phone);

    Optional<Volunteer> findByEmail(String email);

    long countByStatus(VolunteerStatus status);

    /**
     * Find nearest available volunteer with boat capability within radius.
     */
    @Query(value = """
        SELECT v.* FROM volunteers v
        WHERE v.status IN ('ACTIVE', 'STANDBY')
        AND v.has_boat = true
        AND v.current_latitude IS NOT NULL
        AND (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(v.current_latitude)) *
                cos(radians(v.current_longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(v.current_latitude))
            )
        ) <= :radiusKm
        ORDER BY (
            6371 * acos(
                cos(radians(:lat)) * cos(radians(v.current_latitude)) *
                cos(radians(v.current_longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(v.current_latitude))
            )
        ) ASC
        LIMIT 5
        """, nativeQuery = true)
    List<Volunteer> findNearestAvailableBoatRescuers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Find medical volunteers available in a zone.
     */
    @Query("SELECT v FROM Volunteer v WHERE v.isMedicalTrained = true AND v.status IN ('ACTIVE', 'STANDBY') AND v.assignedZone = :zone")
    List<Volunteer> findAvailableMedicalVolunteersInZone(@Param("zone") String zone);

    @Query("SELECT v FROM Volunteer v JOIN v.skills s WHERE s = :skill AND v.status IN ('ACTIVE', 'STANDBY')")
    List<Volunteer> findBySkill(@Param("skill") String skill);
}
