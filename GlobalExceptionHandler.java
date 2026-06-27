package com.floodguard;

import com.floodguard.entity.Resource;
import com.floodguard.enums.ResourceCategory;
import com.floodguard.repository.ResourceRepository;
import com.floodguard.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock ResourceRepository resourceRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks ResourceService resourceService;

    private Resource medicalResource;

    @BeforeEach
    void setUp() {
        medicalResource = Resource.builder()
                .id(1L)
                .name("Insulin vials")
                .category(ResourceCategory.MEDICAL)
                .totalQuantity(200)
                .availableQuantity(80)
                .unit("vials")
                .criticalThreshold(20)
                .warningThreshold(60)
                .build();
    }

    @Test
    void updateQuantity_belowCritical_shouldFireKafkaAlert() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(medicalResource));
        when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Drop below critical threshold
        resourceService.updateAvailableQuantity(1L, 15, "stock used");

        verify(kafkaTemplate, times(1)).send(eq("resource-updates"), eq("1"), any());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/alerts/resources"), any(Object.class));
    }

    @Test
    void updateQuantity_aboveCritical_shouldNotFireKafkaAlert() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(medicalResource));
        when(resourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Stay above critical
        resourceService.updateAvailableQuantity(1L, 50, null);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void getUtilisationPercent_returnsCorrectValue() {
        medicalResource.setAvailableQuantity(100);
        assertThat(medicalResource.getUtilisationPercent()).isEqualTo(50);
    }

    @Test
    void isCriticallyLow_trueWhenBelowThreshold() {
        medicalResource.setAvailableQuantity(15);
        assertThat(medicalResource.isCriticallyLow()).isTrue();
    }

    @Test
    void isCriticallyLow_falseWhenAboveThreshold() {
        medicalResource.setAvailableQuantity(100);
        assertThat(medicalResource.isCriticallyLow()).isFalse();
    }
}
