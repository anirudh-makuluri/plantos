package com.example.plantos.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MachineControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void registersAndListsMachineThroughAllApplicationLayers() throws Exception {
		mockMvc.perform(post("/api/machines")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "TEST-CONTROLLER-01",
							  "name": "Controller Test Machine"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.status").value("OFFLINE"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		mockMvc.perform(get("/api/machines").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].code", hasItem("TEST-CONTROLLER-01")));
	}

	@Test
	void updatesMachineStatusThroughAllApplicationLayers() throws Exception {
		mockMvc.perform(post("/api/machines")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "TEST-UPDATE-01",
							  "name": "Update Test Machine"
							}
							"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OFFLINE"));

		mockMvc.perform(patch("/api/machines/TEST-UPDATE-01/status")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "status": "RUNNING"
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("TEST-UPDATE-01"))
				.andExpect(jsonPath("$.status").value("RUNNING"));
	}

	@Test
	void deletesMachineThroughAllApplicationLayers() throws Exception {
		mockMvc.perform(post("/api/machines")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "TEST-DELETE-01",
							  "name": "Delete Test Machine"
							}
							"""))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/machines/TEST-DELETE-01"))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/machines/TEST-DELETE-01"))
				.andExpect(status().isNotFound());
	}
}
