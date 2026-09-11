package com.example.plantos.backend.controller;

import com.example.plantos.backend.machine.Machine;
import com.example.plantos.backend.machine.MachineService;
import com.example.plantos.backend.machine.MachineStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MachineController.class)
class MachineControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MachineService machineService;

	@Test
	void registersMachineFromJsonRequest() throws Exception {
		Machine savedMachine = new Machine("PRESS-01", "Hydraulic Press", MachineStatus.OFFLINE);
		given(machineService.registerMachine("PRESS-01", "Hydraulic Press"))
				.willReturn(savedMachine);

		mockMvc.perform(post("/api/machines")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "PRESS-01",
							  "name": "Hydraulic Press"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("PRESS-01"))
				.andExpect(jsonPath("$.name").value("Hydraulic Press"))
				.andExpect(jsonPath("$.status").value("OFFLINE"));

		then(machineService).should().registerMachine("PRESS-01", "Hydraulic Press");
	}

	@Test
	void listsMachinesAsJson() throws Exception {
		given(machineService.getMachines()).willReturn(List.of(
				new Machine("PRESS-01", "Hydraulic Press", MachineStatus.RUNNING)
		));

		mockMvc.perform(get("/api/machines").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("PRESS-01"))
				.andExpect(jsonPath("$[0].status").value("RUNNING"));
	}

	@Test
	void changesMachineStatusFromJsonRequest() throws Exception {
		Machine machine = new Machine("PRESS-01", "Hydraulic Press", MachineStatus.RUNNING);
		given(machineService.updateMachineStatus("PRESS-01", MachineStatus.RUNNING))
				.willReturn(Optional.of(machine));

		mockMvc.perform(patch("/api/machines/PRESS-01/status")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "status": "RUNNING"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("PRESS-01"))
				.andExpect(jsonPath("$.status").value("RUNNING"));

		then(machineService).should().updateMachineStatus("PRESS-01", MachineStatus.RUNNING);
	}

	@Test
	void returnsNotFoundWhenChangingAnUnknownMachine() throws Exception {
		given(machineService.updateMachineStatus("UNKNOWN", MachineStatus.RUNNING))
				.willReturn(Optional.empty());

		mockMvc.perform(patch("/api/machines/UNKNOWN/status")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "status": "RUNNING"
							}
							"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAnUpdateWithoutAStatus() throws Exception {
		mockMvc.perform(patch("/api/machines/PRESS-01/status")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deletesAnExistingMachine() throws Exception {
		given(machineService.deleteMachine("PRESS-01")).willReturn(true);

		mockMvc.perform(delete("/api/machines/PRESS-01"))
				.andExpect(status().isNoContent());

		then(machineService).should().deleteMachine("PRESS-01");
	}

	@Test
	void returnsNotFoundWhenDeletingAnUnknownMachine() throws Exception {
		given(machineService.deleteMachine("UNKNOWN")).willReturn(false);

		mockMvc.perform(delete("/api/machines/UNKNOWN"))
				.andExpect(status().isNotFound());
	}
}
