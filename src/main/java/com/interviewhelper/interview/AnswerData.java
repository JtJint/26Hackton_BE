package com.interviewhelper.interview;

import java.time.LocalDateTime;

public record AnswerData(
	Long answerId,
	Long interviewId,
	Long questionId,
	String answerText,
	Integer durationSeconds,
	EyeAnalysis eyeAnalysis,
	SpeechAnalysis speechAnalysis,
	LocalDateTime createdAt
) {
}
