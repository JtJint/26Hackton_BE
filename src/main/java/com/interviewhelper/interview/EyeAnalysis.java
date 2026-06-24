package com.interviewhelper.interview;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EyeAnalysis(
	@DecimalMin("0.0") @DecimalMax("1.0") Double screenFocusRatio,
	@Min(0) Integer gazeAwayCount,
	@Min(0) @Max(100) Integer headMovementScore
) {
}
