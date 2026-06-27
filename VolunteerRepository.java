package com.floodguard;

import com.floodguard.entity.Incident;
import com.floodguard.entity.Volunteer;
import com.floodguard.enums.IncidentStatus;
import com.floodguard.enums.Severity;
import com.floodguard.enums.VolunteerStatus;
import com.floodguard.repository.IncidentRepository;
import com.floodguard.repository.VolunteerRepository;
import com.floodguard.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock IncidentRepository incidentRepository;
    @Mock VolunteerRepository volunteerRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks IncidentService incidentService;

    private Incident sosIncident;
    private Volunteer availableVolunteer;

    @BeforeEach
    void setUp() {
        sosIncident = Incident.builder()
                .id(1L)
                .title("Family trapped")
                .severity(Severity.CRITICAL)
                .status(IncidentStatus.OPEN)
                .latitude(10.787)
                .longitude(76.386)
                .zoneName("Zone A")
                .isSos(true)
                .requiresBoat(true)
                .build();

        availableVolunteer = Volunteer.builder()
                .id(10L)
                .fullName("Arun Menon")
                .status(VolunteerStatus.ACTIVE)
                .hasBoat(true)
                .build();
    }

    @Test
    void createIncident_SOS_shouldPublishToKafkaAndBroadcast() {
        when(incidentRepository.save(any(Incident.class))).thenReturn(sosIncident);
        when(volunteerRepository.findNearestAvailableBoatRescuers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(availableVolunteer));
        when(volunteerRepository.findById(10L)).thenReturn(Optional.of(availableVolunteer));
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sosIncident));

        Incident created = incidentService.createIncident(sosIncident);

        assertThat(created).isNotNull();
        assertThat(created.isSos()).isTrue();
        verify(kafkaTemplate, times(1)).send(eq("sos-alerts"), eq("1"), any());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/incidents"), any(Object.class));
    }

    @Test
    void updateStatus_toResolved_shouldSetResolvedAtAndFreeVolunteer() {
        sosIncident.setAssignedVolunteer(availableVolunteer);
        availableVolunteer.setStatus(VolunteerStatus.DEPLOYED);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sosIncident));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(volunteerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Incident resolved = incidentService.updateStatus(1L, IncidentStatus.RESOLVED, "coordinator1", "All safe");

        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(availableVolunteer.getStatus()).isEqualTo(VolunteerStatus.ACTIVE);
    }

    @Test
    void findById_notFound_shouldThrow() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> incidentService.findById(999L))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("999");
    }

    @Test
    void assignVolunteer_shouldSetDeployedStatus() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(sosIncident));
        when(volunteerRepository.findById(10L)).thenReturn(Optional.of(availableVolunteer));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(volunteerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Incident assigned = incidentService.assignVolunteer(1L, 10L);

        assertThat(assigned.getAssignedVolunteer()).isEqualTo(availableVolunteer);
        assertThat(availableVolunteer.getStatus()).isEqualTo(VolunteerStatus.DEPLOYED);
        assertThat(assigned.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }
}
