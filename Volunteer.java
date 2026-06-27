package com.floodguard.service;

import com.floodguard.entity.Volunteer;
import com.floodguard.enums.VolunteerStatus;
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
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Volunteer register(Volunteer volunteer) {
        if (volunteerRepository.findByPhone(volunteer.getPhone()).isPresent()) {
            throw new IllegalStateException("Phone number already registered: " + volunteer.getPhone());
        }
        Volunteer saved = volunteerRepository.save(volunteer);
        log.info("Volunteer registered: {} ({})", saved.getFullName(), saved.getPhone());
        return saved;
    }

    @Transactional
    public Volunteer updateLocation(Long id, double lat, double lng) {
        Volunteer volunteer = findById(id);
        volunteer.setCurrentLatitude(lat);
        volunteer.setCurrentLongitude(lng);
        volunteer.setLastCheckin(LocalDateTime.now());
        Volunteer updated = volunteerRepository.save(volunteer);

        // Broadcast live location to ops dashboard
        messagingTemplate.convertAndSend("/topic/volunteers/locations", Map.of(
                "volunteerId", id,
                "lat", lat,
                "lng", lng,
                "name", updated.getFullName(),
                "status", updated.getStatus()
        ));

        kafkaTemplate.send("volunteer-updates", String.valueOf(id), Map.of(
                "type", "LOCATION_UPDATE",
                "volunteerId", id,
                "lat", lat,
                "lng", lng
        ));

        return updated;
    }

    @Transactional
    public Volunteer updateStatus(Long id, VolunteerStatus status) {
        Volunteer volunteer = findById(id);
        volunteer.setStatus(status);
        volunteer.setLastCheckin(LocalDateTime.now());
        return volunteerRepository.save(volunteer);
    }

    @Transactional
    public Volunteer assignToZone(Long id, String zone) {
        Volunteer volunteer = findById(id);
        volunteer.setAssignedZone(zone);
        return volunteerRepository.save(volunteer);
    }

    public Volunteer findById(Long id) {
        return volunteerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Volunteer not found: " + id));
    }

    public List<Volunteer> getAll() {
        return volunteerRepository.findAll();
    }

    public List<Volunteer> getByStatus(VolunteerStatus status) {
        return volunteerRepository.findByStatus(status);
    }

    public List<Volunteer> getByZone(String zone) {
        return volunteerRepository.findByAssignedZone(zone);
    }

    public List<Volunteer> getNearestBoatRescuers(double lat, double lng, double radiusKm) {
        return volunteerRepository.findNearestAvailableBoatRescuers(lat, lng, radiusKm);
    }

    public Map<String, Long> getStatusCounts() {
        return Map.of(
                "active", volunteerRepository.countByStatus(VolunteerStatus.ACTIVE),
                "deployed", volunteerRepository.countByStatus(VolunteerStatus.DEPLOYED),
                "standby", volunteerRepository.countByStatus(VolunteerStatus.STANDBY)
        );
    }
}
