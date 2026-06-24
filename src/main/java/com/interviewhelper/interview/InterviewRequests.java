package com.interviewhelper.interview;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class InterviewRequests {

	private InterviewRequests() {
	}

	public record CreateInterviewRequest(
		@NotNull Long resumeId,
		@Min(1) Integer questionCount
	) {
	}

	public record SubmitAnswerRequest(
		@NotNull Long questionId,
		@NotBlank String answerText,
		@Min(0) Integer durationSeconds
	) {
	}

	public record SubmitAnswerAnalysisRequest(
		@NotNull Long questionId,
		@NotBlank String answerText,
		@Min(0) Integer durationSeconds,
		@Valid EyeAnalysis eyeAnalysis,
		@Valid SpeechAnalysis speechAnalysis
	) {
	}

	public record CreateFeedbackRequest(
		List<@NotNull Long> answerIds
	) {
	}
}
