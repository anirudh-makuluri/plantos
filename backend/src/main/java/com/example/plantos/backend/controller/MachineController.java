package com.example.plantos.backend.controller;

import com.example.plantos.backend.dto.CreateMachineRequest;
import com.example.plantos.backend.dto.MachineResponse;
import com.example.plantos.backend.dto.UpdateMachineRequest;
import com.example.plantos.backend.machine.Machine;
import com.example.plantos.backend.machine.MachineService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {
	private final MachineService machineService;

	public MachineController(MachineService machineService) {
		this.machineService = machineService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MachineResponse registerMachine(@RequestBody CreateMachineRequest request) {
		Machine machine = machineService.registerMachine(request.code(), request.name());
		return MachineResponse.from(machine);
	}

	@GetMapping
	public List<MachineResponse> getMachines() {
		return machineService.getMachines().stream()
				.map(MachineResponse::from)
				.toList();
	}

	@PatchMapping("/{code}/status")
	public MachineResponse changeMachineStatus(
			@PathVariable String code,
			@Valid @RequestBody UpdateMachineRequest request
	) {
		Machine machine = machineService.updateMachineStatus(code, request.status())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Machine not found: " + code
				));

		return MachineResponse.from(machine);
	}

	@DeleteMapping("/{code}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMachine(@PathVariable String code) {
		boolean deleted;
		try {
			deleted = machineService.deleteMachine(code);
		} catch (DataIntegrityViolationException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Machine has retained operational history and cannot be deleted", exception);
		}
		if (!deleted) {
			throw new ResponseStatusException(
					HttpStatus.NOT_FOUND,
					"Machine not found: " + code
			);
		}
	}
}
