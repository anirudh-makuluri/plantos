package com.example.plantos.backend.controller;

import com.example.plantos.backend.dto.HealthResponse;
import com.example.plantos.backend.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		return healthService.getHealth();
	}
}
