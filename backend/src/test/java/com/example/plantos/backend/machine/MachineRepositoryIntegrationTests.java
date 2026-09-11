package com.example.plantos.backend.machine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MachineRepositoryIntegrationTests {

	@Autowired
	private MachineRepository machineRepository;

	@Test
	void savesAndFindsMachineByCode() {
		Machine machine = new Machine("TEST-PRESS-01", "Test Hydraulic Press", MachineStatus.IDLE);
		machineRepository.saveAndFlush(machine);

		Optional<Machine> savedMachine = machineRepository.findByCode("TEST-PRESS-01");

		assertThat(savedMachine).isPresent();
		assertThat(savedMachine.orElseThrow().getId()).isNotNull();
		assertThat(savedMachine.orElseThrow().getStatus()).isEqualTo(MachineStatus.IDLE);
	}
}
