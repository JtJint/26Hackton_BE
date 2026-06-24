package com.interviewhelper.interview;

import java.time.LocalDateTime;
import java.util.List;

import com.interviewhelper.resume.CareerLevel;
import com.interviewhelper.resume.InterviewType;
import com.interviewhelper.resume.Position;

public record InterviewData(
	Long interviewId,
	Long resumeId,
	String jobRole,
	CareerLevel careerLevel,
	Position position,
	InterviewType interviewType,
	List<QuestionData> questions,
	LocalDateTime createdAt
) {
}
