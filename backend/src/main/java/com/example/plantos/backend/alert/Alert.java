package com.example.plantos.backend.alert;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String machineCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AnomalyType anomalyType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AlertSeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AlertStatus status;
    @Column(nullable = false)
    private double observedValue;
    @Column(nullable = false)
    private double thresholdValue;
    @Column(nullable = false, length = 10)
    private String unit;
    @Column(nullable = false)
    private int healthScore;
    @Column(nullable = false)
    private Instant firstSeenAt;
    @Column(nullable = false)
    private Instant lastSeenAt;
    @Column(nullable = false)
    private long occurrenceCount;
    @Column(nullable = false)
    private UUID latestEventId;
    private Instant acknowledgedAt;
    private Instant resolvedAt;
    @Column(length = 2000)
    private String resolutionNote;

    protected Alert() {}

    public Alert(MachineAnomalyV1 event) {
        machineCode = event.machineCode();
        anomalyType = event.anomalyType();
        severity = event.severity();
        status = AlertStatus.OPEN;
        firstSeenAt = event.occurredAt();
        lastSeenAt = event.occurredAt();
        observe(event);
    }

    public void observe(MachineAnomalyV1 event) {
        occurrenceCount++;
        if (event.occurredAt().isBefore(firstSeenAt)) {
            firstSeenAt = event.occurredAt();
        }
        // Incident severity retains its highest severity until explicitly resolved.
        if (event.severity() == AlertSeverity.CRITICAL) {
            severity = AlertSeverity.CRITICAL;
        }
        // Late delivery must not replace the most recent measurement with an older one.
        if (!event.occurredAt().isBefore(lastSeenAt)) {
            lastSeenAt = event.occurredAt();
            latestEventId = event.eventId();
            observedValue = event.observedValue();
            thresholdValue = event.thresholdValue();
            unit = event.unit();
            healthScore = event.healthScore();
        }
    }

    public void acknowledge(Instant now) {
        status = AlertStatus.ACKNOWLEDGED;
        acknowledgedAt = now;
    }

    public void resolve(Instant now, String note) {
        status = AlertStatus.RESOLVED;
        resolvedAt = now;
        resolutionNote = note;
    }

    public Long getId() { return id; }
    public String getMachineCode() { return machineCode; }
    public AnomalyType getAnomalyType() { return anomalyType; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public double getObservedValue() { return observedValue; }
    public double getThresholdValue() { return thresholdValue; }
    public String getUnit() { return unit; }
    public int getHealthScore() { return healthScore; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public long getOccurrenceCount() { return occurrenceCount; }
    public UUID getLatestEventId() { return latestEventId; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
}
