package com.floodguard.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${floodguard.kafka.topics.sos-alerts}")
    private String sosAlertsTopic;

    @Value("${floodguard.kafka.topics.resource-updates}")
    private String resourceUpdatesTopic;

    @Value("${floodguard.kafka.topics.volunteer-updates}")
    private String volunteerUpdatesTopic;

    @Value("${floodguard.kafka.topics.incident-broadcasts}")
    private String incidentBroadcastsTopic;

    @Bean
    public NewTopic sosAlertsTopic() {
        return TopicBuilder.name(sosAlertsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic resourceUpdatesTopic() {
        return TopicBuilder.name(resourceUpdatesTopic).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic volunteerUpdatesTopic() {
        return TopicBuilder.name(volunteerUpdatesTopic).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic incidentBroadcastsTopic() {
        return TopicBuilder.name(incidentBroadcastsTopic).partitions(3).replicas(1).build();
    }
}
