package com.floodguard.service;

import com.floodguard.entity.Incident;
import com.floodguard.entity.IncidentUpdate;
import com.floodguard.entity.Volunteer;
import com.floodguard.enums.IncidentStatus;
import com.floodguard.enums.Severity;
import com.floodguard.enums.VolunteerStatus;
import com.floodguard.repository.IncidentRepository;
import com.floodguard.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final VolunteerRepository volunteerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new incident. If it is an SOS, broadcast immediately via
     * WebSocket and Kafka, then attempt auto-assignment.
     */
    @Transactional
    public Incident createIncident(Incident incident) {
        Incident saved = incidentRepository.save(incident);
        log.info("Incident created: id={}, severity={}, isSos={}", saved.getId(), saved.getSeverity(), saved.isSos());

        // Real-time broadcast
        broadcastIncidentUpdate("INCIDENT_CREATED", saved);

        if (saved.isSos()) {
            kafkaTemplate.send("sos-alerts", String.valueOf(saved.getId()), saved);
            log.warn("SOS ALERT dispatched to Kafka: incidentId={}, zone={}", saved.getId(), saved.getZoneName());
            attemptAutoAssignment(saved);
        }

        return saved;
    }

    /**
     * Update incident status. Marks resolvedAt when transitioning to RESOLVED.
     */
    @Transactional
    public Incident updateStatus(Long id, IncidentStatus newStatus, String updatedBy, String note) {
        Incident incident = findById(id);
        IncidentStatus old = incident.getStatus();
        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED && old != IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
            // Free up assigned volunteer
            if (incident.getAssignedVolunteer() != null) {
                Volunteer vol = incident.getAssignedVolunteer();
                vol.setStatus(VolunteerStatus.ACTIVE);
                volunteerRepository.save(vol);
                log.info("Volunteer {} freed from incident {}", vol.getFullName(), id);
            }
        }

        if (note != null && !note.isBlank()) {
            IncidentUpdate update = IncidentUpdate.builder()
                    .incident(incident)
                    .message(note)
                    .updatedBy(updatedBy)
                    .build();
            incident.getUpdates().add(update);
        }

        Incident updated = incidentRepository.save(incident);
        broadcastIncidentUpdate("INCIDENT_STATUS_CHANGED", updated);
        return updated;
    }

    /**
     * Assign a specific volunteer to an incident.
     */
    @Transactional
    public Incident assignVolunteer(Long incidentId, Long volunteerId) {
        Incident incident = findById(incidentId);
        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + volunteerId));

        incident.setAssignedVolunteer(volunteer);
        incident.setStatus(IncidentStatus.ASSIGNED);

        volunteer.setStatus(VolunteerStatus.DEPLOYED);
        volunteerRepository.save(volunteer);

        Incident updated = incidentRepository.save(incident);
        broadcastIncidentUpdate("VOLUNTEER_ASSIGNED", updated);
        log.info("Volunteer {} assigned to incident {}", volunteer.getFullName(), incidentId);
        return updated;
    }

    public Incident findById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found: " + id));
    }

    public List<Incident> getActiveIncidents() {
        return incidentRepository.findByStatusOrderByCreatedAtDesc(IncidentStatus.OPEN);
    }

    public List<Incident> getActiveSosAlerts() {
        return incidentRepository.findByIsSosTrueAndStatusNotOrderByCreatedAtDesc(IncidentStatus.CLOSED);
    }

    public List<Incident> getByZone(String zone) {
        return incidentRepository.findByZoneNameOrderBySeverityDescCreatedAtDesc(zone);
    }

    public List<Incident> getIncidentsNear(double lat, double lng, double radiusKm) {
        return incidentRepository.findIncidentsWithinRadius(lat, lng, radiusKm);
    }

    public Map<String, Long> getSummaryStats() {
        return Map.of(
                "total_active", incidentRepository.countByStatus(IncidentStatus.OPEN),
                "critical", incidentRepository.countBySeverityAndStatus(Severity.CRITICAL, IncidentStatus.OPEN),
                "sos_active", incidentRepository.countByIsSosTrueAndStatusNot(IncidentStatus.CLOSED)
        );
    }

    /**
     * Try to auto-assign the nearest available rescue volunteer to an SOS.
     * Priority: certified rescue workers with boats if requiresBoat.
     */
    private void attemptAutoAssignment(Incident incident) {
        List<Volunteer> candidates;

        if (incident.isRequiresBoat()) {
            candidates = volunteerRepository.findNearestAvailableBoatRescuers(
                    incident.getLatitude(), incident.getLongitude(), 10.0);
        } else if (incident.isRequiresMedical()) {
            candidates = volunteerRepository.findAvailableMedicalVolunteersInZone(
                    incident.getZoneName());
        } else {
            candidates = volunteerRepository.findByStatus(VolunteerStatus.ACTIVE);
        }

        if (!candidates.isEmpty()) {
            Volunteer best = candidates.get(0);
            assignVolunteer(incident.getId(), best.getId());
            log.info("Auto-assigned volunteer {} to SOS incident {}", best.getFullName(), incident.getId());
        } else {
            log.warn("No available volunteers for SOS incident {}", incident.getId());
        }
    }

    private void broadcastIncidentUpdate(String type, Incident incident) {
        messagingTemplate.convertAndSend("/topic/incidents", Map.of(
                "type", type,
                "payload", incident,
                "timestamp", LocalDateTime.now()
        ));
    }
}
