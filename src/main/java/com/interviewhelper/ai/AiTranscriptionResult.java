package com.interviewhelper.ai;

import java.util.List;

public record AiTranscriptionResult(
	String transcript,
	Double durationSeconds,
	Integer paceSpm,
	String paceStatus,
	Integer fillerTotal,
	List<FillerWordResult> fillerWords
) {
	public record FillerWordResult(
		String word,
		Integer count
	) {
	}
}
