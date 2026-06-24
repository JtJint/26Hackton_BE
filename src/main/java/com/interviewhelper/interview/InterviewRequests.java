package com.interviewhelper.interview;

import java.util.List;

import com.interviewhelper.resume.InterviewerType;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class InterviewRequests {

	private InterviewRequests() {
	}

	@Schema(description = "예상 질문 생성 요청")
	public record CreateInterviewRequest(
		@Schema(description = "이력서 저장/분석 응답에서 받은 resumeId", example = "1")
		@NotNull Long resumeId,
		@Schema(description = "생성할 질문 개수. 생략하면 5개, 최대 10개", example = "5")
		@Min(1) Integer questionCount
	) {
	}

	@Schema(description = "MVP 답변 텍스트 저장 요청")
	public record SubmitAnswerRequest(
		@Schema(description = "답변할 질문 ID", example = "101")
		@NotNull Long questionId,
		@Schema(description = "사용자 답변 텍스트", example = "저는 주문 생성 API와 재고 차감 로직을 담당했습니다.")
		@NotBlank String answerText,
		@Schema(description = "답변 소요 시간(초)", example = "72")
		@Min(0) Integer durationSeconds
	) {
	}

	@Schema(description = "시선/음성 분석 지표 포함 답변 저장 요청")
	public record SubmitAnswerAnalysisRequest(
		@Schema(description = "답변할 질문 ID", example = "101")
		@NotNull Long questionId,
		@Schema(description = "사용자 답변 텍스트", example = "저는 주문 생성 API와 재고 차감 로직을 담당했습니다.")
		@NotBlank String answerText,
		@Schema(description = "답변 소요 시간(초)", example = "72")
		@Min(0) Integer durationSeconds,
		@Schema(description = "프론트 MediaPipe 기반 시선 분석 지표")
		@Valid EyeAnalysis eyeAnalysis,
		@Schema(description = "프론트 또는 음성 분석 결과 지표")
		@Valid SpeechAnalysis speechAnalysis
	) {
	}

	@Schema(description = "저장된 답변을 기반으로 실시간 꼬리질문을 생성하는 요청")
	public record CreateFollowUpQuestionRequest(
		@Schema(description = "로그인 응답에서 받은 사용자 ID. MVP에서는 생략 가능", example = "1")
		Long userId,
		@Schema(description = "꼬리질문의 기준이 되는 원본 질문 ID", example = "101")
		@NotNull Long parentQuestionId,
		@Schema(description = "꼬리질문의 기준이 되는 저장된 답변 ID", example = "1001")
		@NotNull Long parentAnswerId,
		@Schema(description = "꼬리질문 생성 시 사용할 면접관 스타일. 생략하면 면접 생성 시 저장된 스타일 사용", example = "TECH_DEEP")
		InterviewerType interviewerType
	) {
	}

	@Schema(description = "면접 피드백 생성 요청")
	public record CreateFeedbackRequest(
		@Schema(description = "로그인 응답에서 받은 사용자 ID. 면접 생성 전 userId 연결이 빠진 경우 대시보드 저장에 사용", example = "1")
		Long userId,
		@Schema(description = "피드백 생성 시 사용할 면접관 스타일. 생략하면 면접 생성 시 저장된 스타일 사용", example = "TECH_DEEP")
		InterviewerType interviewerType,
		@ArraySchema(schema = @Schema(description = "피드백에 포함할 답변 ID", example = "1001"))
		List<@NotNull Long> answerIds
	) {
	}
}
