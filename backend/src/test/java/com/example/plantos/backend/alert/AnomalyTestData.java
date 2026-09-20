package com.example.plantos.backend.alert;

import java.time.Instant;
import java.util.UUID;

final class AnomalyTestData {
    static MachineAnomalyV1 event(String code, Instant time, AlertSeverity severity, double value) {
        UUID source = UUID.randomUUID();
        return new MachineAnomalyV1(UUID.randomUUID(), 1, "MachineAnomaly", time, time,
                code, UUID.randomUUID(), source, source, AnomalyType.HIGH_TEMPERATURE,
                severity, value, severity == AlertSeverity.CRITICAL ? 90.0 : 75.0, "C", 30);
    }

    // Same camelCase names and uppercase enum values as the .NET serializer.
    static final String JSON = """
            {"eventId":"9e2f353b-8f63-4568-9dce-9245126b9001","schemaVersion":1,
             "eventType":"MachineAnomaly","occurredAt":"2026-09-18T10:00:00+00:00",
             "producedAt":"2026-09-18T10:00:01+00:00","machineCode":"TEST-PRESS",
             "correlationId":"9e2f353b-8f63-4568-9dce-9245126b9002",
             "causationId":"9e2f353b-8f63-4568-9dce-9245126b9003",
             "sourceTelemetryEventId":"9e2f353b-8f63-4568-9dce-9245126b9003",
             "anomalyType":"HIGH_TEMPERATURE","severity":"CRITICAL",
             "observedValue":94.83,"thresholdValue":90,"unit":"C","healthScore":34}
            """;
}
