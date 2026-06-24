package com.interviewhelper.resume;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public final class ResumeRequests {

	private ResumeRequests() {
	}

	@Schema(description = "프론트가 파일에서 추출한 이력서 텍스트를 백엔드에 저장할 때 사용하는 요청")
	public record TextResumeRequest(
		@Schema(description = "로그인 응답에서 받은 사용자 ID. 비로그인 MVP 테스트에서는 생략 가능", example = "1")
		Long userId,
		@Schema(description = "지원 직무", example = "백엔드 개발자")
		@NotBlank String jobRole,
		@Schema(description = "경력 수준", example = "JUNIOR")
		CareerLevel careerLevel,
		@Schema(description = "지원 포지션", example = "BACKEND")
		Position position,
		@Schema(description = "면접 단계", example = "TECHNICAL")
		InterviewType interviewType,
		@Schema(description = "프론트에서 추출한 이력서 전체 텍스트", example = "Java와 Spring Boot를 사용해 쇼핑몰 프로젝트를 개발했습니다.")
		@NotBlank String resumeText
	) {
	}

	@Schema(description = "이력서 분석 요청. 질문 생성 전 요약/키워드/추천 토픽을 받을 때 사용")
	public record ResumeAnalysisRequest(
		@Schema(description = "로그인 응답에서 받은 사용자 ID. 비로그인 MVP 테스트에서는 생략 가능", example = "1")
		Long userId,
		@Schema(description = "지원 직무", example = "백엔드 개발자")
		@NotBlank String jobRole,
		@Schema(description = "경력 수준", example = "JUNIOR")
		CareerLevel careerLevel,
		@Schema(description = "지원 포지션", example = "BACKEND")
		Position position,
		@Schema(description = "면접 단계", example = "TECHNICAL")
		InterviewType interviewType,
		@Schema(description = "프론트에서 추출한 이력서 전체 텍스트", example = "Java와 Spring Boot, MySQL을 사용해 주문 API와 재고 차감 로직을 구현했습니다.")
		@NotBlank String resumeText
	) {
	}
}
