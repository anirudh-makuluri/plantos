package com.example.plantos.backend.service;

import com.example.plantos.backend.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

	public HealthResponse getHealth() {
		return new HealthResponse("PlantOS", "UP");
	}
}
