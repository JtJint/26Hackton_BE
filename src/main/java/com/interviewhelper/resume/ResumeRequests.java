package com.interviewhelper.resume;

import jakarta.validation.constraints.NotBlank;

public final class ResumeRequests {

	private ResumeRequests() {
	}

	public record TextResumeRequest(
		@NotBlank String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		@NotBlank String resumeText
	) {
	}

	public record ResumeAnalysisRequest(
		@NotBlank String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		@NotBlank String resumeText
	) {
	}
}
