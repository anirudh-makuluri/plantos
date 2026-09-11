package com.example.plantos.backend.dto;

import com.example.plantos.backend.machine.MachineStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMachineRequest(@NotNull MachineStatus status) {
}
