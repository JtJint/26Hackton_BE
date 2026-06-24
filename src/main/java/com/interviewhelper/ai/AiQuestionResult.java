package com.interviewhelper.ai;

public record AiQuestionResult(
	String externalId,
	String type,
	String content,
	String intent,
	String category
) {
	public AiQuestionResult(String type, String content) {
		this(null, type, content, type, type);
	}
}
