package com.example.plantos.backend.alert;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AlertService {
    private final AlertRepository alerts;
    private final AlertPersistence persistence;
    private final AnomalyParser parser;

    public AlertService(AlertRepository alerts, AlertPersistence persistence, AnomalyParser parser) {
        this.alerts = alerts;
        this.persistence = persistence;
        this.parser = parser;
    }

    @Transactional
    public ProcessingResult process(MachineAnomalyV1 event) {
        parser.validate(event == null ? null : event.machineCode(), event);
        if (!persistence.claimEvent(event)) {
            return ProcessingResult.DUPLICATE;
        }
        if (!persistence.lockMachine(event.machineCode())) {
            throw new InvalidAnomalyException("Register machine before processing anomalies: " + event.machineCode());
        }

        // Delayed events from before a resolution must not resurrect a closed incident.
        var resolved = alerts.findFirstByMachineCodeAndAnomalyTypeAndStatusOrderByResolvedAtDesc(
                event.machineCode(), event.anomalyType(), AlertStatus.RESOLVED);
        if (resolved.isPresent() && !event.occurredAt().isAfter(resolved.get().getResolvedAt())) {
            persistence.audit(resolved.get().getId(), "STALE_IGNORED", "telemetry-processor", event,
                    "Event occurred before the latest resolution");
            return ProcessingResult.STALE;
        }

        var existing = alerts.findActiveForUpdate(event.machineCode(), event.anomalyType());
        Alert alert;
        String action;
        if (existing.isPresent()) {
            alert = existing.get();
            alert.observe(event);
            action = "UPDATED";
        } else {
            alert = alerts.saveAndFlush(new Alert(event));
            action = "CREATED";
        }
        persistence.audit(alert.getId(), action, "telemetry-processor", event, null);
        return existing.isPresent() ? ProcessingResult.UPDATED : ProcessingResult.CREATED;
    }

    public List<Alert> list(String machineCode, AlertStatus status) {
        return alerts.findAlerts(machineCode, status);
    }

    public Alert get(Long id) {
        return alerts.findById(id).orElseThrow(() -> notFound(id));
    }

    public List<AlertPersistence.AuditEntry> history(Long id) {
        get(id);
        return persistence.history(id);
    }

    @Transactional
    public Alert acknowledge(Long id) {
        Alert alert = locked(id);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A resolved alert cannot be acknowledged");
        }
        if (alert.getStatus() == AlertStatus.OPEN) {
            alert.acknowledge(Instant.now());
            // Authentication is milestone 7; this label is deliberately not a claimed user identity.
            persistence.audit(id, "ACKNOWLEDGED", "local-api", null, null);
        }
        return alert;
    }

    @Transactional
    public Alert resolve(Long id, String note) {
        if (note == null || note.isBlank() || note.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resolution requires a note of 1 to 2000 characters");
        }
        Alert alert = locked(id);
        if (alert.getStatus() != AlertStatus.RESOLVED) {
            alert.resolve(Instant.now(), note.strip());
            persistence.audit(id, "RESOLVED", "local-api", null, note.strip());
        }
        return alert;
    }

    private Alert locked(Long id) {
        // Use the same machine lock as event ingestion to serialize resolution with incoming events.
        String code = alerts.findMachineCodeById(id).orElseThrow(() -> notFound(id));
        persistence.lockMachine(code);
        return alerts.findLockedById(id).orElseThrow(() -> notFound(id));
    }

    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found: " + id);
    }

    public enum ProcessingResult { CREATED, UPDATED, DUPLICATE, STALE }
}
