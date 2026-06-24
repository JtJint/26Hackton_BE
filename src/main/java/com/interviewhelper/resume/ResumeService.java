package com.interviewhelper.resume;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.interviewhelper.ai.AiResumeAnalysisResult;
import com.interviewhelper.ai.AiServerClient;
import com.interviewhelper.common.BusinessException;
import com.interviewhelper.resume.ResumeResponses.ResumeAnalysisResponse;

@Service
public class ResumeService {

	private final AiServerClient aiServerClient;
	private final AtomicLong resumeSequence = new AtomicLong(1);
	private final Map<Long, ResumeData> resumes = new ConcurrentHashMap<>();

	public ResumeService(AiServerClient aiServerClient) {
		this.aiServerClient = aiServerClient;
	}

	public ResumeData createFromText(
		Long userId,
		String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		InterviewerType interviewerType,
		String resumeText
	) {
		String normalizedJobRole = requireText(jobRole, "JOB_ROLE_EMPTY", "지원 직무를 입력해 주세요.");
		String normalizedResumeText = requireText(resumeText, "RESUME_TEXT_EMPTY", "이력서 내용을 입력해 주세요.");

		ResumeData resume = new ResumeData(
			resumeSequence.getAndIncrement(),
			userId,
			normalizedJobRole,
			careerLevel == null ? CareerLevel.NEWCOMER : careerLevel,
			position == null ? Position.ETC : position,
			interviewType == null ? InterviewType.TECHNICAL : interviewType,
			interviewerType == null ? InterviewerType.SOFT : interviewerType,
			normalizedResumeText,
			LocalDateTime.now()
		);
		resumes.put(resume.resumeId(), resume);
		return resume;
	}

	public ResumeAnalysisResponse analyze(
		Long userId,
		String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		InterviewerType interviewerType,
		String resumeText
	) {
		ResumeData resume = createFromText(userId, jobRole, careerLevel, position, interviewType, interviewerType, resumeText);
		AiResumeAnalysisResult analysis = aiServerClient.analyzeResume(resume);

		return new ResumeAnalysisResponse(
			resume.resumeId(),
			resume.userId(),
			resume.jobRole(),
			resume.careerLevel(),
			resume.position(),
			resume.interviewType(),
			resume.interviewerType(),
			analysis.summary(),
			analysis.skills(),
			analysis.projectKeywords(),
			analysis.experienceKeywords(),
			analysis.strengths(),
			analysis.improvements(),
			analysis.recommendedQuestionTopics(),
			resume.extractedText(),
			resume.createdAt()
		);
	}

	public ResumeData getResume(Long resumeId) {
		ResumeData resume = resumes.get(resumeId);
		if (resume == null) {
			throw new BusinessException("RESUME_NOT_FOUND", "이력서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
		}
		return resume;
	}

	private String requireText(String value, String code, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
		}
		return value.trim();
	}
}
