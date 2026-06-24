package com.interviewhelper.interview;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewData(
	Long interviewId,
	Long resumeId,
	String jobRole,
	List<QuestionData> questions,
	LocalDateTime createdAt
) {
}
