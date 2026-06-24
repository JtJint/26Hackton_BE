package com.interviewhelper.ai;

public record AiSpeechSynthesisResult(
	String text,
	String audioBase64,
	String contentType,
	String format,
	String voice
) {
}
