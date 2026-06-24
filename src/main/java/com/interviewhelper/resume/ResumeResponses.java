package com.interviewhelper.resume;

import java.time.LocalDateTime;
import java.util.List;

public final class ResumeResponses {

	private ResumeResponses() {
	}

	public record ResumeResponse(
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
		public static ResumeResponse from(ResumeData resume) {
			return new ResumeResponse(
				resume.resumeId(),
				resume.userId(),
				resume.jobRole(),
				resume.careerLevel(),
				resume.position(),
				resume.interviewType(),
				resume.interviewerType(),
				resume.extractedText(),
				resume.createdAt()
			);
		}
	}

	public record ResumeAnalysisResponse(
		Long resumeId,
		Long userId,
		String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		InterviewerType interviewerType,
		String summary,
		List<String> skills,
		List<String> projectKeywords,
		List<String> experienceKeywords,
		List<String> strengths,
		List<String> improvements,
		List<String> recommendedQuestionTopics,
		String extractedText,
		LocalDateTime createdAt
	) {
	}
}
