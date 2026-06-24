package com.interviewhelper.resume;

import java.time.LocalDateTime;

public record ResumeData(
	Long resumeId,
	String jobRole,
	String extractedText,
	LocalDateTime createdAt
) {
}
