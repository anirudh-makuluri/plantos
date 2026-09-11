package com.example.plantos.backend.dto;

import com.example.plantos.backend.machine.Machine;
import com.example.plantos.backend.machine.MachineStatus;

import java.time.Instant;

public record MachineResponse(
		Long id,
		String code,
		String name,
		MachineStatus status,
		Instant createdAt
) {
	public static MachineResponse from(Machine machine) {
		return new MachineResponse(
				machine.getId(),
				machine.getCode(),
				machine.getName(),
				machine.getStatus(),
				machine.getCreatedAt()
		);
	}
}
