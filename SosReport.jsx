package com.floodguard.entity;

import com.floodguard.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "flood_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FloodZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_name", nullable = false, unique = true)
    private String zoneName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Severity severity = Severity.MODERATE;

    // Bounding box for the zone
    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "radius_km")
    private Double radiusKm;

    @Column(name = "water_level_meters")
    private Double waterLevelMeters;

    @Column(name = "is_evacuation_required")
    @Builder.Default
    private boolean evacuationRequired = false;

    @Column(name = "estimated_affected_population")
    private Integer estimatedAffectedPopulation;

    @Column(name = "active_sos_count")
    @Builder.Default
    private Integer activeSosCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
