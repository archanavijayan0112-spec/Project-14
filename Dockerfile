package com.floodguard.entity;

import com.floodguard.enums.ResourceCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Min(0)
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Min(0)
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "unit")
    private String unit;  // packets, litres, units, etc.

    @Column(name = "critical_threshold")
    private Integer criticalThreshold;  // alert when below this

    @Column(name = "warning_threshold")
    private Integer warningThreshold;

    @Column(name = "assigned_to_zone")
    private String assignedToZone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Returns utilisation as a percentage (0–100).
     */
    @Transient
    public int getUtilisationPercent() {
        if (totalQuantity == 0) return 0;
        return (int) (((double) availableQuantity / totalQuantity) * 100);
    }

    @Transient
    public boolean isCriticallyLow() {
        return criticalThreshold != null && availableQuantity <= criticalThreshold;
    }

    @Transient
    public boolean isLow() {
        return warningThreshold != null && availableQuantity <= warningThreshold;
    }
}
