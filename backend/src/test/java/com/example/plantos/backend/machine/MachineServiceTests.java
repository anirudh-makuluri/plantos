package com.example.plantos.backend.machine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MachineServiceTests {

	private final MachineRepository machineRepository = mock(MachineRepository.class);
	private final MachineService machineService = new MachineService(machineRepository);

	@Test
	void registersNewMachinesAsOffline() {
		when(machineRepository.save(any(Machine.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Machine machine = machineService.registerMachine("PRESS-01", "Hydraulic Press");

		assertThat(machine.getCode()).isEqualTo("PRESS-01");
		assertThat(machine.getName()).isEqualTo("Hydraulic Press");
		assertThat(machine.getStatus()).isEqualTo(MachineStatus.OFFLINE);
		verify(machineRepository).save(machine);
	}

	@Test
	void returnsMachinesFromTheRepository() {
		List<Machine> storedMachines = List.of(
				new Machine("PRESS-01", "Hydraulic Press", MachineStatus.RUNNING)
		);
		when(machineRepository.findAll()).thenReturn(storedMachines);

		List<Machine> machines = machineService.getMachines();

		assertThat(machines).isEqualTo(storedMachines);
	}

	@Test
	void updatesTheStatusOfAnExistingMachine() {
		Machine machine = new Machine("PRESS-01", "Hydraulic Press", MachineStatus.OFFLINE);
		when(machineRepository.findByCode("PRESS-01")).thenReturn(Optional.of(machine));

		Optional<Machine> updatedMachine = machineService.updateMachineStatus(
				"PRESS-01",
				MachineStatus.RUNNING
		);

		assertThat(updatedMachine).containsSame(machine);
		assertThat(machine.getStatus()).isEqualTo(MachineStatus.RUNNING);
	}

	@Test
	void returnsEmptyWhenUpdatingAnUnknownMachine() {
		when(machineRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

		Optional<Machine> updatedMachine = machineService.updateMachineStatus(
				"UNKNOWN",
				MachineStatus.RUNNING
		);

		assertThat(updatedMachine).isEmpty();
	}

	@Test
	void deletesAnExistingMachine() {
		Machine machine = new Machine("PRESS-01", "Hydraulic Press", MachineStatus.OFFLINE);
		when(machineRepository.findByCode("PRESS-01")).thenReturn(Optional.of(machine));

		boolean deleted = machineService.deleteMachine("PRESS-01");

		assertThat(deleted).isTrue();
		verify(machineRepository).delete(machine);
	}

	@Test
	void returnsFalseWhenDeletingAnUnknownMachine() {
		when(machineRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

		boolean deleted = machineService.deleteMachine("UNKNOWN");

		assertThat(deleted).isFalse();
	}
}
