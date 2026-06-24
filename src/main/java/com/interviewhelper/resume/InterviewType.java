package com.interviewhelper.resume;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접 단계 또는 면접 유형")
public enum InterviewType {
	@Schema(description = "기술면접")
	TECHNICAL,
	@Schema(description = "인성면접")
	PERSONALITY,
	@Schema(description = "기술 + 인성 종합")
	COMPREHENSIVE
}
