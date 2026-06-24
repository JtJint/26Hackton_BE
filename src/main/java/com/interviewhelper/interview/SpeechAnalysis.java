package com.interviewhelper.interview;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프론트 또는 음성 분석 서버가 계산해서 백엔드로 전달하는 발화 지표")
public record SpeechAnalysis(
	@Schema(description = "분당 단어 수", example = "142")
	@Min(0) Integer wordsPerMinute,
	@Schema(description = "습관어 사용 횟수", example = "6")
	@Min(0) Integer fillerWordCount,
	@Schema(description = "총 침묵 시간(초)", example = "5.2")
	@DecimalMin("0.0") Double silenceSeconds,
	@Schema(description = "음량 안정성 점수. 0~100", example = "76")
	@Min(0) @Max(100) Integer volumeStabilityScore
) {
}
