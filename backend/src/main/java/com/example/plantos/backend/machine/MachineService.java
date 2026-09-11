package com.example.plantos.backend.machine;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MachineService {
	private final MachineRepository machineRepository;

	public MachineService(MachineRepository machineRepository) {
		this.machineRepository = machineRepository;
	}

	@Transactional
	public Machine registerMachine(String code, String name) {
		Machine machine = new Machine(code, name, MachineStatus.OFFLINE);
		return machineRepository.save(machine);
	}

	public List<Machine> getMachines() {
		return machineRepository.findAll();
	}

	public Optional<Machine> findByCode(String code) {
		return machineRepository.findByCode(code);
	}

	@Transactional
	public Optional<Machine> updateMachineStatus(String code, MachineStatus status) {
		return machineRepository.findByCode(code)
				.map(machine -> {
					machine.changeStatus(status);
					return machine;
				});
	}

	@Transactional
	public boolean deleteMachine(String code) {
		return machineRepository.findByCode(code)
				.map(machine -> {
					machineRepository.delete(machine);
					return true;
				})
				.orElse(false);
	}
}
