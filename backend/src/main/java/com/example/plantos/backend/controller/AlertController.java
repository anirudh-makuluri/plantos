package com.example.plantos.backend.controller;

import com.example.plantos.backend.alert.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping("/api/alerts")
    public List<AlertResponse> list(@RequestParam(required = false) String machineCode,
                                    @RequestParam(required = false) AlertStatus status) {
        return service.list(machineCode, status).stream().map(AlertResponse::from).toList();
    }

    @GetMapping("/api/machines/{code}/alerts")
    public List<AlertResponse> machineAlerts(@PathVariable String code,
                                            @RequestParam(required = false) AlertStatus status) {
        return service.list(code, status).stream().map(AlertResponse::from).toList();
    }

    @GetMapping("/api/alerts/{id}")
    public AlertResponse get(@PathVariable Long id) {
        return AlertResponse.from(service.get(id));
    }

    @GetMapping("/api/alerts/{id}/history")
    public List<AlertPersistence.AuditEntry> history(@PathVariable Long id) {
        return service.history(id);
    }

    @PostMapping("/api/alerts/{id}/acknowledgements")
    public AlertResponse acknowledge(@PathVariable Long id) {
        return AlertResponse.from(service.acknowledge(id));
    }

    @PostMapping("/api/alerts/{id}/resolve")
    public AlertResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveAlertRequest request) {
        return AlertResponse.from(service.resolve(id, request.note()));
    }

    public record ResolveAlertRequest(@NotBlank @Size(max = 2000) String note) {}
}
