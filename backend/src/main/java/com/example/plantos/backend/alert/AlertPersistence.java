package com.example.plantos.backend.alert;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** JdbcTemplate participates in the same datasource transaction as Hibernate. */
@Repository
public class AlertPersistence {
    private final JdbcTemplate jdbc;

    public AlertPersistence(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean claimEvent(MachineAnomalyV1 event) {
        // A pre-insert exists() check is not enough: two consumers could both see false.
        return jdbc.update("""
                INSERT INTO processed_events(event_id, machine_code, occurred_at)
                VALUES (?, ?, ?) ON CONFLICT (event_id) DO NOTHING
                """, event.eventId(), event.machineCode(), Timestamp.from(event.occurredAt())) == 1;
    }

    public boolean lockMachine(String code) {
        // Serializes first-alert creation even when there is no alert row to lock yet.
        return !jdbc.queryForList("SELECT id FROM machines WHERE code = ? FOR UPDATE", Long.class, code).isEmpty();
    }

    public void audit(Long alertId, String action, String actor, MachineAnomalyV1 event, String note) {
        jdbc.update("""
                INSERT INTO alert_audit(alert_id, action, actor, event_id, correlation_id, recorded_at, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, alertId, action, actor, event == null ? null : event.eventId(),
                event == null ? null : event.correlationId(), Timestamp.from(Instant.now()), note);
    }

    public List<AuditEntry> history(Long alertId) {
        return jdbc.query("SELECT * FROM alert_audit WHERE alert_id = ? ORDER BY id", (row, index) ->
                new AuditEntry(row.getLong("id"), row.getString("action"), row.getString("actor"),
                        row.getObject("event_id", UUID.class), row.getObject("correlation_id", UUID.class),
                        row.getTimestamp("recorded_at").toInstant(), row.getString("note")), alertId);
    }

    public record AuditEntry(Long id, String action, String actor, UUID eventId,
                             UUID correlationId, Instant recordedAt, String note) {}
}
