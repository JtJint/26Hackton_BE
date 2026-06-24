package com.interviewhelper.ai;

import java.util.List;

public record AiTranscriptionResult(
	String transcript,
	Double durationSeconds,
	Integer paceSpm,
	String paceStatus,
	Integer fillerTotal,
	Integer repetitionCount,
	Integer selfCorrectionCount,
	Integer longPauseCount,
	Double maxPauseSeconds,
	Double avgPauseSeconds,
	Integer disfluencyScore,
	List<FillerWordResult> fillerWords
) {
	public record FillerWordResult(
		String word,
		Integer count
	) {
	}
}
