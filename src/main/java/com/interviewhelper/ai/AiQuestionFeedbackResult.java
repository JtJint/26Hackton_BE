package com.interviewhelper.ai;

public record AiQuestionFeedbackResult(
	Long questionId,
	String question,
	String answer,
	Integer score,
	String feedback
) {
}
