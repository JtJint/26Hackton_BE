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

	public record FeedbackDetailResponse(
		Integer score,
		String strength,
		String improvement
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
		FeedbackDetailResponse contentFeedback,
		FeedbackDetailResponse eyeFeedback,
		FeedbackDetailResponse speechFeedback,
		String recommendedAnswer,
		String gapCriterion,
		String followUpQuestion,
		List<QuestionLog> questionLogs,
		LocalDateTime createdAt
	) {
		public static RecentPracticeResponse from(PracticeResultEntity result, List<QuestionLog> questionLogs) {
			return new RecentPracticeResponse(
				result.getId(),
				result.getInterviewId(),
				result.getTotalScore(),
				result.getContentScore(),
				result.getEyeScore(),
				result.getSpeechScore(),
				result.getSummary(),
				new FeedbackDetailResponse(result.getContentScore(), result.getContentStrength(), result.getContentImprovement()),
				new FeedbackDetailResponse(result.getEyeScore(), result.getEyeStrength(), result.getEyeImprovement()),
				new FeedbackDetailResponse(result.getSpeechScore(), result.getSpeechStrength(), result.getSpeechImprovement()),
				result.getRecommendedAnswer(),
				result.getGapCriterion(),
				result.getFollowUpQuestion(),
				questionLogs,
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
