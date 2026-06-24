package com.interviewhelper.interview;

public record QuestionData(
	Long questionId,
	Integer order,
	String type,
	String content
) {
}
