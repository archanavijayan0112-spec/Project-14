package com.floodguard.dto;

import com.floodguard.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

// ─── Incident DTOs ───────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class IncidentRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    private String zoneName;
    private String reportedBy;
    private String contactNumber;
    private Integer affectedPeople;
    private boolean isSos;
    private boolean requiresBoat;
    private boolean requiresMedical;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class IncidentResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Severity severity;
    private IncidentStatus status;
    private Double latitude;
    private Double longitude;
    private String zoneName;
    private String reportedBy;
    private String contactNumber;
    private Integer affectedPeople;
    private boolean isSos;
    private boolean requiresBoat;
    private boolean requiresMedical;
    private String assignedVolunteerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<IncidentUpdateDTO> updates;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class IncidentUpdateDTO {
    private Long id;
    private String message;
    private String updatedBy;
    private LocalDateTime createdAt;
}

// ─── Volunteer DTOs ──────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class VolunteerRequestDTO {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phone;

    @Email
    private String email;

    private List<String> skills;
    private String assignedZone;
    private Double currentLatitude;
    private Double currentLongitude;
    private boolean certifiedRescue;
    private boolean medicalTrained;
    private boolean hasBoat;
    private boolean hasVehicle;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class VolunteerResponseDTO {
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private List<String> skills;
    private VolunteerStatus status;
    private String assignedZone;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastCheckin;
    private boolean certifiedRescue;
    private boolean medicalTrained;
    private boolean hasBoat;
    private boolean hasVehicle;
    private int activeIncidentCount;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class VolunteerLocationUpdateDTO {
    @NotNull private Double latitude;
    @NotNull private Double longitude;
}

// ─── Resource DTOs ───────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResourceRequestDTO {
    @NotBlank private String name;
    @NotNull  private ResourceCategory category;
    private String locationName;
    private Double locationLatitude;
    private Double locationLongitude;
    @Min(0) @NotNull private Integer totalQuantity;
    @Min(0) @NotNull private Integer availableQuantity;
    private String unit;
    private Integer criticalThreshold;
    private Integer warningThreshold;
    private String assignedToZone;
    private String notes;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResourceResponseDTO {
    private Long id;
    private String name;
    private ResourceCategory category;
    private String locationName;
    private Double locationLatitude;
    private Double locationLongitude;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private String unit;
    private Integer criticalThreshold;
    private Integer warningThreshold;
    private String assignedToZone;
    private String notes;
    private int utilisationPercent;
    private boolean criticallyLow;
    private boolean low;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResourceUpdateQuantityDTO {
    @Min(0) @NotNull private Integer availableQuantity;
    private String notes;
}

// ─── FloodZone DTOs ──────────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FloodZoneResponseDTO {
    private Long id;
    private String zoneName;
    private Severity severity;
    private Double centerLat;
    private Double centerLng;
    private Double radiusKm;
    private Double waterLevelMeters;
    private boolean evacuationRequired;
    private Integer estimatedAffectedPopulation;
    private Integer activeSosCount;
    private LocalDateTime updatedAt;
}

// ─── Auth DTOs ───────────────────────────────────────────────────────────────

@Data
@NoArgsConstructor
@AllArgsConstructor
class LoginRequestDTO {
    @NotBlank private String username;
    @NotBlank private String password;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoginResponseDTO {
    private String token;
    private String username;
    private List<String> roles;
    private long expiresIn;
}

// ─── WebSocket message ───────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WebSocketMessage {
    private String type;     // SOS_ALERT, RESOURCE_UPDATE, VOLUNTEER_UPDATE, INCIDENT_UPDATE
    private Object payload;
    private LocalDateTime timestamp;
}

// ─── Dashboard summary ───────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DashboardSummaryDTO {
    private long totalActiveIncidents;
    private long criticalIncidents;
    private long activeSosAlerts;
    private long totalVolunteers;
    private long deployedVolunteers;
    private long openShelters;
    private long totalEvacuated;
    private double riverWaterLevelMeters;
    private double rainfall24h;
    private long affectedZones;
    private long criticallyLowResources;
}
