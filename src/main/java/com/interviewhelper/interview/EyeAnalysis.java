package com.interviewhelper.interview;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프론트 MediaPipe가 계산해서 백엔드로 전달하는 시선/얼굴 지표")
public record EyeAnalysis(
	@Schema(description = "전체 답변 시간 중 화면을 바라본 비율. 0~1", example = "0.74")
	@DecimalMin("0.0") @DecimalMax("1.0") Double screenFocusRatio,
	@Schema(description = "시선 이탈 횟수", example = "8")
	@Min(0) Integer gazeAwayCount,
	@Schema(description = "고개 움직임 안정성 점수. 0~100", example = "82")
	@Min(0) @Max(100) Integer headMovementScore
) {
}
