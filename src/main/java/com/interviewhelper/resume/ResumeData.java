package com.interviewhelper.resume;

import java.time.LocalDateTime;

public record ResumeData(
	Long resumeId,
	Long userId,
	String jobRole,
	CareerLevel careerLevel,
	Position position,
	InterviewType interviewType,
	InterviewerType interviewerType,
	String extractedText,
	LocalDateTime createdAt
) {
}
