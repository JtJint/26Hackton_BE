package com.interviewhelper.interview;

public record QuestionData(
	Long questionId,
	Integer order,
	String type,
	String content,
	String intent,
	String category
) {
	public QuestionData(Long questionId, Integer order, String type, String content) {
		this(questionId, order, type, content, type, type);
	}
}
