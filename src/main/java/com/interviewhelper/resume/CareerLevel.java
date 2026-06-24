package com.interviewhelper.resume;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지원자의 경력 수준")
public enum CareerLevel {
	@Schema(description = "신입")
	NEWCOMER,
	@Schema(description = "경력")
	EXPERIENCED
}