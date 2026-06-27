package com.floodguard.service;

import com.floodguard.entity.Resource;
import com.floodguard.enums.ResourceCategory;
import com.floodguard.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Resource createResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    @Transactional
    public Resource updateAvailableQuantity(Long id, int newQty, String notes) {
        Resource resource = findById(id);
        int previousQty = resource.getAvailableQuantity();
        resource.setAvailableQuantity(newQty);
        if (notes != null) resource.setNotes(notes);

        Resource updated = resourceRepository.save(resource);

        // Fire alert if stock just crossed threshold
        if (updated.isCriticallyLow() && !isCriticallyLow(previousQty, resource)) {
            broadcastResourceAlert("CRITICAL_LOW", updated);
            kafkaTemplate.send("resource-updates", String.valueOf(id), Map.of(
                    "type", "CRITICAL_LOW",
                    "resourceId", id,
                    "name", resource.getName(),
                    "available", newQty
            ));
            log.warn("CRITICAL LOW resource: {} ({} {} remaining)", resource.getName(), newQty, resource.getUnit());
        }

        broadcastResourceUpdate(updated);
        return updated;
    }

    @Transactional
    public Resource allocateToZone(Long id, String zone) {
        Resource resource = findById(id);
        resource.setAssignedToZone(zone);
        return resourceRepository.save(resource);
    }

    public Resource findById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
    }

    public List<Resource> getAll() {
        return resourceRepository.findAll();
    }

    public List<Resource> getByCategory(ResourceCategory category) {
        return resourceRepository.findByCategory(category);
    }

    public List<Resource> getCriticallyLow() {
        return resourceRepository.findCriticallyLowResources();
    }

    public List<Resource> getLowResources() {
        return resourceRepository.findLowResources();
    }

    /**
     * Scheduled check every 15 minutes — re-broadcast any critical resources
     * in case the frontend missed the initial alert.
     */
    @Scheduled(fixedDelay = 900_000)
    public void scheduledResourceCheck() {
        List<Resource> critical = getCriticallyLow();
        if (!critical.isEmpty()) {
            log.warn("Scheduled check: {} resources critically low", critical.size());
            critical.forEach(r -> broadcastResourceAlert("CRITICAL_LOW_REMINDER", r));
        }
    }

    private boolean isCriticallyLow(int qty, Resource r) {
        return r.getCriticalThreshold() != null && qty <= r.getCriticalThreshold();
    }

    private void broadcastResourceUpdate(Resource resource) {
        messagingTemplate.convertAndSend("/topic/resources", Map.of(
                "type", "RESOURCE_UPDATED",
                "payload", resource
        ));
    }

    private void broadcastResourceAlert(String alertType, Resource resource) {
        messagingTemplate.convertAndSend("/topic/alerts/resources", Map.of(
                "type", alertType,
                "resourceId", resource.getId(),
                "name", resource.getName(),
                "category", resource.getCategory(),
                "available", resource.getAvailableQuantity(),
                "unit", resource.getUnit()
        ));
    }
}
