package com.interviewhelper.dashboard;

public record QuestionLog(
	Long mainQuestionId,
	String mainQuestion,
	String mainAnswer,
	Long followUpQuestionId,
	String followUpQuestion,
	String followUpAnswer
) {
}
