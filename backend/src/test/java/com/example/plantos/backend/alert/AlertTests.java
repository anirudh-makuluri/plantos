package com.example.plantos.backend.alert;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTests {

    @Test
    void initializesFromTheFirstAnomaly() {
        Instant occurredAt = Instant.parse("2026-09-26T10:00:00Z");
        MachineAnomalyV1 event = AnomalyTestData.event("PRESS-01", occurredAt, AlertSeverity.WARNING, 80);

        Alert alert = new Alert(event);

        assertThat(alert.getMachineCode()).isEqualTo("PRESS-01");
        assertThat(alert.getAnomalyType()).isEqualTo(AnomalyType.HIGH_TEMPERATURE);
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(alert.getFirstSeenAt()).isEqualTo(occurredAt);
        assertThat(alert.getLastSeenAt()).isEqualTo(occurredAt);
        assertThat(alert.getOccurrenceCount()).isOne();
        assertThat(alert.getObservedValue()).isEqualTo(80);
        assertThat(alert.getThresholdValue()).isEqualTo(75);
        assertThat(alert.getLatestEventId()).isEqualTo(event.eventId());
    }

    @Test
    void keepsTheLatestMeasurementButRetainsCriticalSeverity() {
        Instant firstTime = Instant.parse("2026-09-26T10:00:00Z");
        Alert alert = new Alert(AnomalyTestData.event("PRESS-01", firstTime,
                AlertSeverity.WARNING, 80));
        MachineAnomalyV1 critical = AnomalyTestData.event("PRESS-01", firstTime.plusSeconds(10),
                AlertSeverity.CRITICAL, 95);

        alert.observe(critical);

        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.getObservedValue()).isEqualTo(95);
        assertThat(alert.getOccurrenceCount()).isEqualTo(2);
        assertThat(alert.getLatestEventId()).isEqualTo(critical.eventId());
    }

    @Test
    void lateMeasurementUpdatesOccurrenceButDoesNotRegressCurrentReading() {
        Instant latestTime = Instant.parse("2026-09-26T10:00:10Z");
        Alert alert = new Alert(AnomalyTestData.event("PRESS-01", latestTime,
                AlertSeverity.WARNING, 80));
        MachineAnomalyV1 lateEvent = AnomalyTestData.event("PRESS-01", latestTime.minusSeconds(10),
                AlertSeverity.WARNING, 70);

        alert.observe(lateEvent);

        assertThat(alert.getOccurrenceCount()).isEqualTo(2);
        assertThat(alert.getFirstSeenAt()).isEqualTo(lateEvent.occurredAt());
        assertThat(alert.getLastSeenAt()).isEqualTo(latestTime);
        assertThat(alert.getObservedValue()).isEqualTo(80);
    }

    @Test
    void acknowledgesAndResolvesAnAlert() {
        Alert alert = new Alert(AnomalyTestData.event("PRESS-01", Instant.now(),
                AlertSeverity.WARNING, 80));
        Instant acknowledgedAt = Instant.parse("2026-09-26T10:01:00Z");
        Instant resolvedAt = Instant.parse("2026-09-26T10:02:00Z");

        alert.acknowledge(acknowledgedAt);
        alert.resolve(resolvedAt, "Fault cleared");

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getAcknowledgedAt()).isEqualTo(acknowledgedAt);
        assertThat(alert.getResolvedAt()).isEqualTo(resolvedAt);
        assertThat(alert.getResolutionNote()).isEqualTo("Fault cleared");
    }
}
