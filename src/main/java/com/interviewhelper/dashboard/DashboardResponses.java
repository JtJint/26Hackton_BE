package com.interviewhelper.dashboard;

import java.time.LocalDateTime;
import java.util.List;

public final class DashboardResponses {

	private DashboardResponses() {
	}

	public record AreaScores(
		Integer content,
		Integer eye,
		Integer speech
	) {
	}

	public record RecentPracticeResponse(
		Long resultId,
		Long interviewId,
		Integer totalScore,
		Integer contentScore,
		Integer eyeScore,
		Integer speechScore,
		String summary,
		LocalDateTime createdAt
	) {
		public static RecentPracticeResponse from(PracticeResultEntity result) {
			return new RecentPracticeResponse(
				result.getId(),
				result.getInterviewId(),
				result.getTotalScore(),
				result.getContentScore(),
				result.getEyeScore(),
				result.getSpeechScore(),
				result.getSummary(),
				result.getCreatedAt()
			);
		}
	}

	public record DashboardResponse(
		Long userId,
		Integer practiceCount,
		Integer averageScore,
		Integer scoreTrend,
		String weakestArea,
		String weakestAreaLabel,
		String recommendedPractice,
		AreaScores areaAverages,
		List<RecentPracticeResponse> recentPractices
	) {
	}
}
