package com.interviewhelper.resume;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "면접관 스타일")
public enum InterviewerType {
	@Schema(description = "부드럽고 균형 잡힌 면접관")
	SOFT,
	@Schema(description = "답변의 근거와 모호한 부분을 압박하는 면접관")
	PRESSURE,
	@Schema(description = "기술 깊이와 대안 비교를 파고드는 면접관")
	TECH_DEEP,
	@Schema(description = "인성, 협업, 갈등 해결을 중심으로 보는 면접관")
	PERSONALITY
}
