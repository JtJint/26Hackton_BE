package com.interviewhelper.ai;

import java.util.List;

public record AiResumeAnalysisResult(
	String summary,
	List<String> skills,
	List<String> projectKeywords,
	List<String> experienceKeywords,
	List<String> strengths,
	List<String> improvements,
	List<String> recommendedQuestionTopics
) {
}
