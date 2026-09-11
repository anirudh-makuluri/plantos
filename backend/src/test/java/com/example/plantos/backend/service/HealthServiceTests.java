package com.example.plantos.backend.service;

import com.example.plantos.backend.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthServiceTests {

	private final HealthService healthService = new HealthService();

	@Test
	void reportsThatPlantOsIsUp() {
		HealthResponse response = healthService.getHealth();

		assertThat(response.application()).isEqualTo("PlantOS");
		assertThat(response.status()).isEqualTo("UP");
	}
}
