package com.floodguard.websocket;

import com.floodguard.entity.Incident;
import com.floodguard.service.IncidentService;
import com.floodguard.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Handles STOMP messages sent from clients to /app/...
 * The SimpMessagingTemplate is used to push to /topic/... subscribers.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DisasterWebSocketHandler {

    private final IncidentService incidentService;
    private final VolunteerService volunteerService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends SOS to /app/sos
     * Broadcast to all dashboard subscribers on /topic/incidents
     */
    @MessageMapping("/sos")
    @SendTo("/topic/incidents")
    public Map<String, Object> handleSos(Incident incident) {
        log.warn("WebSocket SOS received: {}", incident.getTitle());
        Incident saved = incidentService.createIncident(incident);
        return Map.of(
                "type", "SOS_RECEIVED",
                "incidentId", saved.getId(),
                "title", saved.getTitle(),
                "severity", saved.getSeverity(),
                "lat", saved.getLatitude(),
                "lng", saved.getLongitude()
        );
    }

    /**
     * Volunteer location ping: /app/volunteer/location
     */
    @MessageMapping("/volunteer/location")
    public void handleVolunteerLocation(@Payload Map<String, Object> payload) {
        Long volunteerId = ((Number) payload.get("volunteerId")).longValue();
        Double lat = (Double) payload.get("lat");
        Double lng = (Double) payload.get("lng");

        volunteerService.updateLocation(volunteerId, lat, lng);
        // updateLocation already broadcasts to /topic/volunteers/locations
    }

    /**
     * General incident update ping: /app/incident/update
     */
    @MessageMapping("/incident/update")
    @SendTo("/topic/incidents")
    public Map<String, Object> handleIncidentUpdate(@Payload Map<String, Object> payload) {
        return Map.of(
                "type", "INCIDENT_UPDATE",
                "payload", payload
        );
    }
}
