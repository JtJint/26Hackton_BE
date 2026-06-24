package com.interviewhelper.interview;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SpeechAnalysis(
	@Min(0) Integer wordsPerMinute,
	@Min(0) Integer fillerWordCount,
	@DecimalMin("0.0") Double silenceSeconds,
	@Min(0) @Max(100) Integer volumeStabilityScore
) {
}
