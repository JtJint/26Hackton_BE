package com.interviewhelper.interview;

import java.util.List;

import com.interviewhelper.interview.InterviewResponses.FeedbackCategoryResponse;
import com.interviewhelper.interview.InterviewResponses.QuestionResultResponse;

public record FeedbackData(
	Long interviewId,
	Integer totalScore,
	String summary,
	FeedbackCategoryResponse contentFeedback,
	FeedbackCategoryResponse eyeFeedback,
	FeedbackCategoryResponse speechFeedback,
	String recommendedAnswer,
	String gapCriterion,
	String followUpQuestion,
	List<QuestionResultResponse> questionResults
) {
}
