package com.example.plantos.backend.controller;

import com.example.plantos.backend.dto.HealthResponse;
import com.example.plantos.backend.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HealthService healthService;

	@Test
	void returnsApplicationHealthAsJson() throws Exception {
		given(healthService.getHealth()).willReturn(new HealthResponse("PlantOS", "UP"));

		mockMvc.perform(get("/api/health").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.application").value("PlantOS"))
				.andExpect(jsonPath("$.status").value("UP"));

		then(healthService).should().getHealth();
	}
}
