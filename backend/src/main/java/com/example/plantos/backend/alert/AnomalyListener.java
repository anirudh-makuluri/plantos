package com.example.plantos.backend.alert;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "plantos.kafka.enabled", havingValue = "true")
public class AnomalyListener {
    private static final Logger log = LoggerFactory.getLogger(AnomalyListener.class);
    private final AnomalyParser parser;
    private final AlertService service;

    public AnomalyListener(AnomalyParser parser, AlertService service) {
        this.parser = parser;
        this.service = service;
    }

    @KafkaListener(id = "plantos-anomalies", topics = "${plantos.kafka.anomaly-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        MachineAnomalyV1 event = parser.parse(record.key(), record.value());
        // Calling a separate @Transactional bean commits the database before this listener returns.
        var result = service.process(event);
        log.info("Anomaly result={} eventId={} correlationId={} machineCode={} partition={} offset={}",
                result, event.eventId(), event.correlationId(), event.machineCode(), record.partition(), record.offset());
    }
}
