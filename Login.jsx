package com.floodguard.entity;

import com.floodguard.enums.IncidentStatus;
import com.floodguard.enums.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    // Geospatial coordinates
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "reported_by")
    private String reportedBy;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "affected_people")
    private Integer affectedPeople;

    @Column(name = "is_sos")
    @Builder.Default
    private boolean isSos = false;

    @Column(name = "requires_boat")
    @Builder.Default
    private boolean requiresBoat = false;

    @Column(name = "requires_medical")
    @Builder.Default
    private boolean requiresMedical = false;

    // Assigned volunteer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_volunteer_id")
    private Volunteer assignedVolunteer;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IncidentUpdate> updates = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
