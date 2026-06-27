package com.floodguard.service;

import com.floodguard.enums.IncidentStatus;
import com.floodguard.enums.Severity;
import com.floodguard.enums.VolunteerStatus;
import com.floodguard.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final VolunteerRepository volunteerRepository;
    private final ResourceRepository resourceRepository;
    private final FloodZoneRepository floodZoneRepository;

    public Map<String, Object> getSummary() {
        return Map.of(
                "incidents", Map.of(
                        "total_active", incidentRepository.countByStatus(IncidentStatus.OPEN),
                        "critical", incidentRepository.countBySeverityAndStatus(Severity.CRITICAL, IncidentStatus.OPEN),
                        "sos_active", incidentRepository.countByIsSosTrueAndStatusNot(IncidentStatus.CLOSED)
                ),
                "volunteers", Map.of(
                        "total", volunteerRepository.count(),
                        "active", volunteerRepository.countByStatus(VolunteerStatus.ACTIVE),
                        "deployed", volunteerRepository.countByStatus(VolunteerStatus.DEPLOYED),
                        "standby", volunteerRepository.countByStatus(VolunteerStatus.STANDBY)
                ),
                "resources", Map.of(
                        "critically_low", resourceRepository.findCriticallyLowResources().size(),
                        "low", resourceRepository.findLowResources().size()
                ),
                "zones", Map.of(
                        "total", floodZoneRepository.count(),
                        "critical", floodZoneRepository.countBySeverityIn(List.of(Severity.CRITICAL, Severity.HIGH)),
                        "evacuation_required", floodZoneRepository.findByEvacuationRequiredTrue().size()
                )
        );
    }
}
