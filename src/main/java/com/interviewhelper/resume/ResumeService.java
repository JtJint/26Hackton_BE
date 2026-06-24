package com.interviewhelper.resume;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.interviewhelper.common.BusinessException;
import com.interviewhelper.resume.ResumeResponses.ResumeAnalysisResponse;

@Service
public class ResumeService {

	private static final List<String> KNOWN_SKILLS = List.of(
		"Spring Boot", "Spring", "Java", "JPA", "MySQL", "Redis", "Docker",
		"AWS", "Kubernetes", "REST API", "Git", "Python", "React", "TypeScript"
	);

	private final AtomicLong resumeSequence = new AtomicLong(1);
	private final Map<Long, ResumeData> resumes = new ConcurrentHashMap<>();

	public ResumeData createFromText(String jobRole, String resumeText) {
		String normalizedJobRole = requireText(jobRole, "JOB_ROLE_EMPTY", "지원 직무를 입력해 주세요.");
		String normalizedResumeText = requireText(resumeText, "RESUME_TEXT_EMPTY", "이력서 내용을 입력해 주세요.");

		ResumeData resume = new ResumeData(
			resumeSequence.getAndIncrement(),
			normalizedJobRole,
			normalizedResumeText,
			LocalDateTime.now()
		);
		resumes.put(resume.resumeId(), resume);
		return resume;
	}

	public ResumeAnalysisResponse analyze(String jobRole, String resumeText) {
		ResumeData resume = createFromText(jobRole, resumeText);
		List<String> skills = extractSkills(resume.extractedText());
		List<String> projectKeywords = extractSentences(resume.extractedText(), List.of("프로젝트", "개발", "구현", "api"));
		List<String> experienceKeywords = extractSentences(resume.extractedText(), List.of("개선", "성능", "해결", "운영", "장애", "협업", "문제"));

		String skillSummary = skills.isEmpty() ? "명확히 드러난 기술 키워드는 아직 적습니다." : "주요 기술은 " + String.join(", ", skills) + "입니다.";

		return new ResumeAnalysisResponse(
			resume.resumeId(),
			resume.jobRole(),
			resume.jobRole() + " 이력서로 분석되었고, " + skillSummary,
			skills,
			projectKeywords,
			experienceKeywords,
			List.of(
				"이력서 내용을 기반으로 직무 맞춤형 질문을 만들 수 있습니다.",
				"프로젝트와 경험 문장을 중심으로 답변을 준비하기 좋습니다."
			),
			List.of("면접 답변에서는 본인의 기여도와 결과를 한 문장으로 먼저 말하는 연습이 필요합니다."),
			recommendTopics(resume.jobRole(), skills),
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

	private List<String> extractSkills(String text) {
		String lowerText = text.toLowerCase(Locale.ROOT);
		LinkedHashSet<String> skills = new LinkedHashSet<>();

		for (String skill : KNOWN_SKILLS) {
			if (lowerText.contains(skill.toLowerCase(Locale.ROOT))) {
				skills.add(skill);
			}
		}

		return new ArrayList<>(skills);
	}

	private List<String> extractSentences(String text, List<String> keywords) {
		String[] sentences = text.split("[.!?\\n]+");
		List<String> results = new ArrayList<>();

		for (String sentence : sentences) {
			String normalized = sentence.trim();
			String lowerSentence = normalized.toLowerCase(Locale.ROOT);
			if (!normalized.isBlank() && keywords.stream().anyMatch(lowerSentence::contains)) {
				results.add(normalized);
			}
		}

		if (results.isEmpty() && !text.isBlank()) {
			results.add(text.length() > 120 ? text.substring(0, 120) : text);
		}

		return results.stream().limit(3).toList();
	}

	private List<String> recommendTopics(String jobRole, List<String> skills) {
		List<String> topics = new ArrayList<>();
		topics.add(jobRole + " 직무 지원 동기");
		skills.stream()
			.limit(3)
			.map(skill -> skill + " 사용 이유와 장단점")
			.forEach(topics::add);
		topics.add("대표 프로젝트에서 본인이 담당한 핵심 기능");
		return topics;
	}

	private String requireText(String value, String code, String message) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(code, message, HttpStatus.BAD_REQUEST);
		}
		return value.trim();
	}
}
