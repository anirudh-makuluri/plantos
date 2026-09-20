package com.example.plantos.backend.alert;

import com.example.plantos.backend.machine.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "plantos.kafka.enabled=false")
@AutoConfigureMockMvc
class AlertWorkflowIntegrationTests {
    @Autowired AlertService service;
    @Autowired MachineRepository machines;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoSpyBean AlertPersistence persistence;
    private String code;

    @BeforeEach
    void createMachine() {
        code = "M3-TEST-" + UUID.randomUUID();
        machines.saveAndFlush(new Machine(code, "Milestone 3 test", MachineStatus.RUNNING));
    }

    @AfterEach
    void removeOnlyThisTestsRows() {
        jdbc.update("DELETE FROM alert_audit WHERE alert_id IN (SELECT id FROM alerts WHERE machine_code = ?)", code);
        jdbc.update("DELETE FROM alerts WHERE machine_code = ?", code);
        jdbc.update("DELETE FROM processed_events WHERE machine_code = ?", code);
        jdbc.update("DELETE FROM machines WHERE code = ?", code);
    }

    @Test
    void replayHasNoDuplicateBusinessEffectsAndNewEventsUpdateTheIncident() {
        var event = event(Instant.now().minusSeconds(10), AlertSeverity.CRITICAL, 95);
        assertThat(service.process(event)).isEqualTo(AlertService.ProcessingResult.CREATED);
        for (int i = 0; i < 5; i++) {
            assertThat(service.process(event)).isEqualTo(AlertService.ProcessingResult.DUPLICATE);
        }
        assertThat(service.list(code, null)).singleElement().satisfies(alert -> {
            assertThat(alert.getOccurrenceCount()).isEqualTo(1);
            assertThat(service.history(alert.getId())).hasSize(1);
        });
        service.process(event(Instant.now(), AlertSeverity.WARNING, 80));
        assertThat(service.list(code, null)).singleElement().satisfies(alert -> {
            assertThat(alert.getOccurrenceCount()).isEqualTo(2);
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(alert.getObservedValue()).isEqualTo(80);
            assertThat(service.history(alert.getId())).hasSize(2);
        });
        assertThat(machines.findByCode(code).orElseThrow().getStatus()).isEqualTo(MachineStatus.RUNNING);
    }

    @Test
    void failedAuditRollsBackTheEventAndAlertSoTheSameEventCanBeRetried() {
        var event = event(Instant.now(), AlertSeverity.CRITICAL, 95);
        doThrow(new DataIntegrityViolationException("Simulated audit failure")).when(persistence)
                .audit(anyLong(), anyString(), anyString(), any(), isNull());
        assertThatThrownBy(() -> service.process(event)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.list(code, null)).isEmpty();
        assertThat(eventCount()).isZero();
        reset(persistence);
        assertThat(service.process(event)).isEqualTo(AlertService.ProcessingResult.CREATED);
    }

    @Test
    void unregisteredMachineDoesNotClaimTheEvent() {
        machines.delete(machines.findByCode(code).orElseThrow());
        var event = event(Instant.now(), AlertSeverity.CRITICAL, 95);
        assertThatThrownBy(() -> service.process(event)).isInstanceOf(InvalidAnomalyException.class);
        assertThat(eventCount()).isZero();
        machines.saveAndFlush(new Machine(code, "Registered after failure", MachineStatus.IDLE));
        assertThat(service.process(event)).isEqualTo(AlertService.ProcessingResult.CREATED);
    }

    @Test
    void concurrentReplayCreatesOneAlertOneAuditAndOneProcessedEvent() throws Exception {
        var event = event(Instant.now(), AlertSeverity.CRITICAL, 95);
        concurrently(() -> service.process(event), () -> service.process(event));
        assertThat(eventCount()).isEqualTo(1);
        assertThat(service.list(code, null)).singleElement().satisfies(alert -> {
            assertThat(alert.getOccurrenceCount()).isEqualTo(1);
            assertThat(service.history(alert.getId())).hasSize(1);
        });
    }

    @Test
    void concurrentDifferentEventsShareOneActiveIncidentWithoutLosingUpdates() throws Exception {
        var first = event(Instant.now().minusSeconds(1), AlertSeverity.WARNING, 80);
        var second = event(Instant.now(), AlertSeverity.CRITICAL, 95);
        concurrently(() -> service.process(first), () -> service.process(second));
        assertThat(eventCount()).isEqualTo(2);
        assertThat(service.list(code, null)).singleElement().satisfies(alert -> {
            assertThat(alert.getOccurrenceCount()).isEqualTo(2);
            assertThat(alert.getObservedValue()).isEqualTo(95);
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        });
    }

    @Test
    void acknowledgementAndResolutionAreAuditedAndRepeatedCommandsAreHarmless() throws Exception {
        service.process(event(Instant.now().minusSeconds(5), AlertSeverity.CRITICAL, 95));
        long id = service.list(code, null).getFirst().getId();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/alerts/{id}/acknowledgements", id))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
        }
        // A subsequent reading updates the incident without erasing acknowledgement.
        service.process(event(Instant.now().minusSeconds(1), AlertSeverity.CRITICAL, 96));
        assertThat(service.get(id).getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        mvc.perform(post("/api/alerts/{id}/resolve", id).contentType(MediaType.APPLICATION_JSON).content("{\"note\":\" \"}"))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/alerts/{id}/resolve", id).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"note\":\"Inspected and reset simulated fault\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
        }
        mvc.perform(post("/api/alerts/{id}/acknowledgements", id)).andExpect(status().isConflict());
        mvc.perform(get("/api/alerts/{id}/history", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].actor").value("local-api"));
        mvc.perform(get("/api/alerts").param("machineCode", code).param("status", "RESOLVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/machines/{code}/alerts", code).param("status", "OPEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/machines/{code}", code)).andExpect(status().isConflict());
    }

    @Test
    void lateEventsDoNotRegressReadingsOrReopenResolvedIncidents() {
        Instant now = Instant.now();
        service.process(event(now.minusSeconds(10), AlertSeverity.CRITICAL, 95));
        service.process(event(now.minusSeconds(20), AlertSeverity.WARNING, 80));
        var alert = service.list(code, null).getFirst();
        assertThat(alert.getObservedValue()).isEqualTo(95);
        assertThat(alert.getOccurrenceCount()).isEqualTo(2);
        service.resolve(alert.getId(), "Fault cleared");
        var delayed = event(now.minusSeconds(5), AlertSeverity.CRITICAL, 99);
        assertThat(service.process(delayed)).isEqualTo(AlertService.ProcessingResult.STALE);
        assertThat(service.process(delayed)).isEqualTo(AlertService.ProcessingResult.DUPLICATE);
        assertThat(service.list(code, AlertStatus.OPEN)).isEmpty();
        assertThat(service.history(alert.getId())).hasSize(4);
        var recurrence = event(service.get(alert.getId()).getResolvedAt().plusSeconds(1), AlertSeverity.WARNING, 80);
        assertThat(service.process(recurrence)).isEqualTo(AlertService.ProcessingResult.CREATED);
        assertThat(service.list(code, null)).hasSize(2);
    }

    @Test
    void returnsNotFoundForUnknownAlertAndRejectsUnknownStatus() throws Exception {
        mvc.perform(get("/api/alerts/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/api/alerts/{id}/history", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(post("/api/alerts/{id}/acknowledgements", Long.MAX_VALUE)).andExpect(status().isNotFound());
        mvc.perform(get("/api/alerts").param("status", "BOGUS")).andExpect(status().isBadRequest());
    }

    private MachineAnomalyV1 event(Instant time, AlertSeverity severity, double value) {
        return AnomalyTestData.event(code, time, severity, value);
    }

    private long eventCount() {
        return jdbc.queryForObject("SELECT count(*) FROM processed_events WHERE machine_code = ?", Long.class, code);
    }

    private void concurrently(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> { start.await(); return first.call(); });
            var b = executor.submit(() -> { start.await(); return second.call(); });
            start.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        }
    }
}
