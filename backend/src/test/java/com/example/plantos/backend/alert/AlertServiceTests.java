package com.example.plantos.backend.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

    @Mock
    private AlertRepository alerts;
    @Mock
    private AlertPersistence persistence;
    @Mock
    private AnomalyParser parser;

    private AlertService service;
    private final MachineAnomalyV1 event = AnomalyTestData.event(
            "PRESS-01", Instant.parse("2026-09-26T10:00:00Z"), AlertSeverity.WARNING, 80);

    @BeforeEach
    void setUp() {
        service = new AlertService(alerts, persistence, parser);
        lenient().when(persistence.claimEvent(any(MachineAnomalyV1.class))).thenReturn(true);
        lenient().when(persistence.lockMachine(anyString())).thenReturn(true);
        lenient().when(alerts.findFirstByMachineCodeAndAnomalyTypeAndStatusOrderByResolvedAtDesc(
            anyString(), any(AnomalyType.class), eq(AlertStatus.RESOLVED)))
                .thenReturn(Optional.empty());
    }

    @Test
    void createsAnAlertForAClaimedEventWhenNoActiveAlertExists() {
        when(alerts.findActiveForUpdate("PRESS-01", AnomalyType.HIGH_TEMPERATURE))
                .thenReturn(Optional.empty());
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertService.ProcessingResult result = service.process(event);

        assertThat(result).isEqualTo(AlertService.ProcessingResult.CREATED);
        verify(parser).validate("PRESS-01", event);
        verify(alerts).saveAndFlush(any(Alert.class));
        verify(persistence).audit(any(), eq("CREATED"), eq("telemetry-processor"), eq(event), eq(null));
    }

    @Test
    void updatesAnExistingActiveAlert() {
        Alert existing = new Alert(event);
        when(alerts.findActiveForUpdate("PRESS-01", AnomalyType.HIGH_TEMPERATURE))
                .thenReturn(Optional.of(existing));
        MachineAnomalyV1 newerEvent = AnomalyTestData.event(
                "PRESS-01", event.occurredAt().plusSeconds(1), AlertSeverity.CRITICAL, 95);

        AlertService.ProcessingResult result = service.process(newerEvent);

        assertThat(result).isEqualTo(AlertService.ProcessingResult.UPDATED);
        assertThat(existing.getOccurrenceCount()).isEqualTo(2);
        assertThat(existing.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        verify(alerts, never()).saveAndFlush(any(Alert.class));
    }

    @Test
    void returnsDuplicateWithoutLockingWhenEventWasAlreadyClaimed() {
        when(persistence.claimEvent(event)).thenReturn(false);

        AlertService.ProcessingResult result = service.process(event);

        assertThat(result).isEqualTo(AlertService.ProcessingResult.DUPLICATE);
        verify(persistence, never()).lockMachine(anyString());
        verify(alerts, never()).findActiveForUpdate(anyString(), any(AnomalyType.class));
    }

    @Test
    void rejectsAnEventForAnUnknownMachine() {
        when(persistence.lockMachine("PRESS-01")).thenReturn(false);

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(InvalidAnomalyException.class)
                .hasMessageContaining("Register machine before processing anomalies");
        verify(alerts, never()).saveAndFlush(any(Alert.class));
    }

    @Test
    void ignoresAnEventOlderThanTheLatestResolution() {
        Alert resolved = new Alert(event);
        resolved.resolve(Instant.parse("2026-09-26T10:00:10Z"), "Cleared");
        when(alerts.findFirstByMachineCodeAndAnomalyTypeAndStatusOrderByResolvedAtDesc(
                "PRESS-01", AnomalyType.HIGH_TEMPERATURE, AlertStatus.RESOLVED))
                .thenReturn(Optional.of(resolved));

        AlertService.ProcessingResult result = service.process(event);

        assertThat(result).isEqualTo(AlertService.ProcessingResult.STALE);
        verify(persistence).audit(any(), eq("STALE_IGNORED"), eq("telemetry-processor"), eq(event),
                eq("Event occurred before the latest resolution"));
        verify(alerts, never()).findActiveForUpdate(anyString(), any(AnomalyType.class));
    }

    @Test
    void listsAlertsAndLoadsHistoryOnlyAfterConfirmingTheAlertExists() {
        Alert alert = new Alert(event);
        List<Alert> expected = List.of(alert);
        when(alerts.findAlerts("PRESS-01", AlertStatus.OPEN)).thenReturn(expected);
        when(alerts.findById(7L)).thenReturn(Optional.of(alert));
        List<AlertPersistence.AuditEntry> history = List.of();
        when(persistence.history(7L)).thenReturn(history);

        assertThat(service.list("PRESS-01", AlertStatus.OPEN)).isSameAs(expected);
        assertThat(service.history(7L)).isSameAs(history);
        verify(persistence).history(7L);
    }

    @Test
    void throwsNotFoundForUnknownAlert() {
        when(alerts.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void acknowledgesOnlyOpenAlertsAndRejectsResolvedAlerts() {
        Alert open = new Alert(event);
        when(alerts.findMachineCodeById(1L)).thenReturn(Optional.of("PRESS-01"));
        when(alerts.findLockedById(1L)).thenReturn(Optional.of(open));

        assertThat(service.acknowledge(1L).getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        verify(persistence).audit(1L, "ACKNOWLEDGED", "local-api", null, null);

        Alert resolved = new Alert(event);
        resolved.resolve(Instant.now(), "Cleared");
        when(alerts.findLockedById(1L)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> service.acknowledge(1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void validatesResolutionNotesAndResolvesAnAlert() {
        assertThatThrownBy(() -> service.resolve(1L, " "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Alert open = new Alert(event);
        when(alerts.findMachineCodeById(1L)).thenReturn(Optional.of("PRESS-01"));
        when(alerts.findLockedById(1L)).thenReturn(Optional.of(open));

        Alert result = service.resolve(1L, "  Fault cleared  ");

        assertThat(result.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(result.getResolutionNote()).isEqualTo("Fault cleared");
        verify(persistence).audit(1L, "RESOLVED", "local-api", null, "Fault cleared");
    }
}
