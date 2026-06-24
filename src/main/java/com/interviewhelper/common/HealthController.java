package com.interviewhelper.common;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class HealthController {

	@GetMapping({"/", "/api/health"})
	public Map<String, String> health() {
		return Map.of(
			"status", "ok",
			"message", "interview-helper backend is running"
		);
	}
}
