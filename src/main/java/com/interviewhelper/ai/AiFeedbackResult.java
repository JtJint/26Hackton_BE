package com.interviewhelper.ai;

import java.util.List;

public record AiFeedbackResult(
	Integer totalScore,
	String summary,
	AiFeedbackCategoryResult contentFeedback,
	AiFeedbackCategoryResult eyeFeedback,
	AiFeedbackCategoryResult speechFeedback,
	String recommendedAnswer,
	List<AiQuestionFeedbackResult> questionResults
) {
}
