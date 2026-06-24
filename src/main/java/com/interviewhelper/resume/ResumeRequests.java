package com.interviewhelper.resume;

import jakarta.validation.constraints.NotBlank;

public final class ResumeRequests {

	private ResumeRequests() {
	}

	public record TextResumeRequest(
		@NotBlank String jobRole,
		@NotBlank String resumeText
	) {
	}

	public record ResumeAnalysisRequest(
		@NotBlank String jobRole,
		@NotBlank String resumeText
	) {
	}
}
