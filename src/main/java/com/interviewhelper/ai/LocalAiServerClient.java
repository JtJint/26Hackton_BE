package com.interviewhelper.ai;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.interviewhelper.interview.AnswerData;
import com.interviewhelper.interview.EyeAnalysis;
import com.interviewhelper.interview.InterviewData;
import com.interviewhelper.interview.QuestionData;
import com.interviewhelper.interview.SpeechAnalysis;
import com.interviewhelper.resume.CareerLevel;
import com.interviewhelper.resume.InterviewType;
import com.interviewhelper.resume.Position;
import com.interviewhelper.resume.ResumeData;

public class LocalAiServerClient implements AiServerClient {

	private static final List<String> KNOWN_SKILLS = List.of(
		"Spring Boot", "Spring", "Java", "JPA", "MySQL", "Redis", "Docker",
		"AWS", "Kubernetes", "REST API", "Git", "Python", "React", "TypeScript"
	);

	@Override
	public AiResumeAnalysisResult analyzeResume(ResumeData resume) {
		List<String> skills = extractSkills(resume.extractedText());
		List<String> projectKeywords = extractSentences(resume.extractedText(), List.of("프로젝트", "개발", "구현", "api"));
		List<String> experienceKeywords = extractSentences(resume.extractedText(), List.of("개선", "성능", "해결", "운영", "장애", "협업", "문제"));
		String skillSummary = skills.isEmpty() ? "명확히 드러난 기술 키워드는 아직 적습니다." : "주요 기술은 " + String.join(", ", skills) + "입니다.";

		return new AiResumeAnalysisResult(
			resume.jobRole() + " 이력서로 분석되었고, " + skillSummary,
			skills,
			projectKeywords,
			experienceKeywords,
			List.of(
				"이력서 내용을 기반으로 직무 맞춤형 질문을 만들 수 있습니다.",
				"프로젝트와 경험 문장을 중심으로 답변을 준비하기 좋습니다."
			),
			List.of("면접 답변에서는 본인의 기여도와 결과를 한 문장으로 먼저 말하는 연습이 필요합니다."),
			recommendTopics(resume, skills)
		);
	}

	@Override
	public List<AiQuestionResult> generateQuestions(ResumeData resume, int questionCount) {
		List<AiQuestionResult> templates = questionTemplates(resume);
		List<AiQuestionResult> questions = new ArrayList<>();

		for (int index = 0; index < questionCount; index++) {
			questions.add(templates.get(index % templates.size()));
		}

		return questions;
	}

	@Override
	public AiTranscriptionResult transcribeAudio(String filename, String contentType, byte[] audioBytes) {
		return new AiTranscriptionResult(
			"로컬 대체 STT 결과입니다.",
			0.0,
			320,
			"ideal",
			0,
			0,
			0,
			0,
			0.0,
			0.0,
			0,
			List.of()
		);
	}

	@Override
	public AiSpeechSynthesisResult synthesizeSpeech(String text, String voice, String instructions) {
		return new AiSpeechSynthesisResult(
			text,
			Base64.getEncoder().encodeToString(("local tts: " + text).getBytes(StandardCharsets.UTF_8)),
			"audio/mpeg",
			"mp3",
			voice == null || voice.isBlank() ? "nova" : voice
		);
	}

	@Override
	public AiFeedbackResult generateFeedback(InterviewData interview, List<AnswerData> answers) {
		List<AnswerData> sortedAnswers = answers.stream()
			.sorted(Comparator.comparing(AnswerData::createdAt))
			.toList();
		int contentScore = scoreContent(sortedAnswers);
		int eyeScore = scoreEye(sortedAnswers);
		int speechScore = scoreSpeech(sortedAnswers);
		int totalScore = clamp((contentScore + eyeScore + speechScore) / 3);

		return new AiFeedbackResult(
			totalScore,
			createSummary(totalScore),
			new AiFeedbackCategoryResult(
				contentScore,
				"답변에서 담당 역할과 구현 내용을 설명했습니다.",
				"성과, 수치, 문제 해결 과정을 함께 말하면 설득력이 높아집니다."
			),
			new AiFeedbackCategoryResult(
				eyeScore,
				"대부분의 답변 흐름에서 화면 응시를 유지했습니다.",
				"핵심 문장을 말할 때 시선이 흔들리지 않도록 짧은 문장 단위로 연습해 보세요."
			),
			new AiFeedbackCategoryResult(
				speechScore,
				"말 속도와 음량 흐름은 면접 상황에 사용할 수 있는 수준입니다.",
				"습관어를 줄이고 문장 사이에 의도적인 쉼을 넣으면 더 안정적으로 들립니다."
			),
			createRecommendedAnswer(interview, sortedAnswers),
			createQuestionResults(interview, sortedAnswers, contentScore)
		);
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

	private List<String> recommendTopics(ResumeData resume, List<String> skills) {
		List<String> topics = new ArrayList<>();
		topics.add(resume.jobRole() + " 직무 지원 동기");
		if (resume.careerLevel() == CareerLevel.EXPERIENCED) {
			topics.add("경력 프로젝트에서 맡은 역할과 의사결정 근거");
		}
		skills.stream()
			.limit(3)
			.map(skill -> skill + " 사용 이유와 장단점")
			.forEach(topics::add);
		topics.add(positionTopic(resume.position()));
		return topics;
	}

	private String positionTopic(Position position) {
		return switch (position) {
			case BACKEND -> "대표 프로젝트의 API 설계, DB, 트랜잭션 처리 경험";
			case FRONTEND -> "대표 프로젝트의 UI 상태 관리와 사용자 경험 개선 경험";
			case FULLSTACK -> "프론트엔드와 백엔드를 연결하며 맡은 전체 흐름";
			case AI -> "모델 또는 AI API를 서비스에 적용한 경험";
			case DEVOPS -> "배포, 모니터링, 장애 대응 경험";
			case MOBILE -> "모바일 앱 구조와 사용자 단말 환경 대응 경험";
			case ETC -> "대표 프로젝트에서 본인이 담당한 핵심 기능";
		};
	}

	private List<AiQuestionResult> questionTemplates(ResumeData resume) {
		List<AiQuestionResult> templates = new ArrayList<>();

		if (resume.interviewType() == InterviewType.TECHNICAL || resume.interviewType() == InterviewType.COMPREHENSIVE) {
			templates.addAll(technicalTemplates(resume));
		}

		if (resume.interviewType() == InterviewType.PERSONALITY || resume.interviewType() == InterviewType.COMPREHENSIVE) {
			templates.addAll(personalityTemplates(resume));
		}

		templates.add(new AiQuestionResult("JOB_ROLE", resume.jobRole() + " 직무에 지원한 동기와 본인의 강점은 무엇인가요?"));
		return templates;
	}

	private List<AiQuestionResult> technicalTemplates(ResumeData resume) {
		List<AiQuestionResult> templates = new ArrayList<>();
		templates.add(new AiQuestionResult("PROJECT", "대표 프로젝트에서 본인이 담당한 핵심 기능은 무엇인가요?"));
		templates.add(new AiQuestionResult("TECH", positionTechnicalQuestion(resume.position())));
		templates.add(new AiQuestionResult("EXPERIENCE", "프로젝트 진행 중 가장 어려웠던 문제와 해결 방법을 설명해 주세요."));

		if (resume.careerLevel() == CareerLevel.EXPERIENCED) {
			templates.add(new AiQuestionResult("TECH", "기술 선택 과정에서 고려한 트레이드오프와 의사결정 근거를 설명해 주세요."));
			templates.add(new AiQuestionResult("EXPERIENCE", "운영 중 발생한 장애나 성능 문제를 어떻게 분석하고 개선했나요?"));
		}
		else {
			templates.add(new AiQuestionResult("TECH", "사용한 기술의 동작 원리를 본인 프로젝트 사례로 설명해 주세요."));
			templates.add(new AiQuestionResult("EXPERIENCE", "최근 학습한 기술을 프로젝트에 적용한 경험이 있나요?"));
		}

		templates.add(new AiQuestionResult("PROJECT", "본인이 구현한 기능의 예외 상황과 테스트 방법을 설명해 주세요."));
		return templates;
	}

	private List<AiQuestionResult> personalityTemplates(ResumeData resume) {
		List<AiQuestionResult> templates = new ArrayList<>();
		templates.add(new AiQuestionResult("PERSONALITY", "협업 과정에서 의견 충돌이 있었을 때 어떻게 해결했나요?"));
		templates.add(new AiQuestionResult("PERSONALITY", "실패하거나 아쉬웠던 경험과 그 이후 바꾼 행동을 설명해 주세요."));

		if (resume.careerLevel() == CareerLevel.EXPERIENCED) {
			templates.add(new AiQuestionResult("PERSONALITY", "이직 또는 다음 커리어에서 가장 중요하게 보는 기준은 무엇인가요?"));
			templates.add(new AiQuestionResult("PERSONALITY", "업무 우선순위가 충돌할 때 어떤 기준으로 판단하나요?"));
		}
		else {
			templates.add(new AiQuestionResult("PERSONALITY", "새로운 기술을 학습할 때 어떤 방식으로 계획하고 검증하나요?"));
			templates.add(new AiQuestionResult("PERSONALITY", "본인의 성장 가능성을 보여주는 경험을 설명해 주세요."));
		}

		return templates;
	}

	private String positionTechnicalQuestion(Position position) {
		return switch (position) {
			case BACKEND -> "API 설계, DB 트랜잭션, 성능 개선 중 가장 자신 있는 경험을 설명해 주세요.";
			case FRONTEND -> "상태 관리, 렌더링 최적화, 사용자 경험 개선 중 가장 자신 있는 경험을 설명해 주세요.";
			case FULLSTACK -> "프론트엔드와 백엔드 사이의 데이터 흐름을 어떻게 설계하고 연결했나요?";
			case AI -> "AI 모델 또는 외부 AI API를 서비스에 적용할 때 고려한 품질 기준은 무엇인가요?";
			case DEVOPS -> "배포 자동화, 모니터링, 장애 대응 중 직접 개선한 경험을 설명해 주세요.";
			case MOBILE -> "모바일 환경에서 성능, 네트워크, 사용자 경험을 개선한 경험을 설명해 주세요.";
			case ETC -> "지원 포지션에서 가장 자신 있는 기술 경험과 그 이유는 무엇인가요?";
		};
	}

	private int scoreContent(List<AnswerData> targetAnswers) {
		double averageLength = targetAnswers.stream()
			.mapToInt(answer -> answer.answerText().length())
			.average()
			.orElse(0);
		return clamp((int) Math.round(55 + Math.min(35, averageLength / 4)));
	}

	private int scoreEye(List<AnswerData> targetAnswers) {
		List<EyeAnalysis> analyses = targetAnswers.stream()
			.map(AnswerData::eyeAnalysis)
			.filter(analysis -> analysis != null)
			.toList();

		if (analyses.isEmpty()) {
			return 75;
		}

		double average = analyses.stream()
			.mapToDouble(analysis -> {
				double focus = valueOrDefault(analysis.screenFocusRatio(), 0.7) * 100;
				double head = valueOrDefault(analysis.headMovementScore(), 75);
				double gazePenalty = valueOrDefault(analysis.gazeAwayCount(), 0) * 1.5;
				return focus * 0.55 + head * 0.45 - gazePenalty;
			})
			.average()
			.orElse(75);

		return clamp((int) Math.round(average));
	}

	private int scoreSpeech(List<AnswerData> targetAnswers) {
		List<SpeechAnalysis> analyses = targetAnswers.stream()
			.map(AnswerData::speechAnalysis)
			.filter(analysis -> analysis != null)
			.toList();

		if (analyses.isEmpty()) {
			return 75;
		}

		double average = analyses.stream()
			.mapToDouble(analysis -> {
				double wpm = valueOrDefault(analysis.wordsPerMinute(), 135);
				double speedScore = Math.max(55, 100 - Math.abs(wpm - 140) * 0.8);
				double fillerPenalty = valueOrDefault(analysis.fillerWordCount(), 0) * 2.0;
				double silencePenalty = valueOrDefault(analysis.silenceSeconds(), 0.0) * 1.2;
				double volume = valueOrDefault(analysis.volumeStabilityScore(), 75);
				return speedScore * 0.45 + volume * 0.35 + 20 - fillerPenalty - silencePenalty;
			})
			.average()
			.orElse(75);

		return clamp((int) Math.round(average));
	}

	private List<AiQuestionFeedbackResult> createQuestionResults(InterviewData interview, List<AnswerData> targetAnswers, int contentScore) {
		return targetAnswers.stream()
			.map(answer -> {
				QuestionData question = interview.questions()
					.stream()
					.filter(candidate -> candidate.questionId().equals(answer.questionId()))
					.findFirst()
					.orElseThrow();
				return new AiQuestionFeedbackResult(
					question.questionId(),
					question.content(),
					answer.answerText(),
					contentScore,
					answer.answerText().toLowerCase(Locale.ROOT).contains("개선")
						? "개선 경험이 드러납니다. 결과 지표까지 덧붙이면 더 좋습니다."
						: "담당 기능은 설명했지만 문제 상황, 행동, 결과 순서로 정리하면 더 명확합니다."
				);
			})
			.toList();
	}

	private String createSummary(int totalScore) {
		if (totalScore >= 85) {
			return "답변 흐름이 안정적이고 직무 경험을 구체적으로 설명했습니다.";
		}
		if (totalScore >= 70) {
			return "프로젝트 경험은 설명했지만, 성과와 수치가 더 보완되면 좋습니다.";
		}
		return "답변의 구조와 핵심 근거를 보완하면 면접 설득력이 좋아집니다.";
	}

	private String createRecommendedAnswer(InterviewData interview, List<AnswerData> targetAnswers) {
		String baseAnswer = targetAnswers.get(0).answerText();
		String shortened = baseAnswer.length() > 80 ? baseAnswer.substring(0, 80) : baseAnswer;
		String normalized = shortened.replaceFirst("^저는\\s+", "");
		return "저는 " + interview.jobRole() + " 관점에서 " + normalized
			+ " 이 경험을 문제 상황, 제가 맡은 역할, 적용한 해결 방법, 결과 순서로 설명하겠습니다.";
	}

	private int clamp(int score) {
		return Math.max(0, Math.min(100, score));
	}

	private double valueOrDefault(Number value, double defaultValue) {
		return value == null ? defaultValue : value.doubleValue();
	}
}
