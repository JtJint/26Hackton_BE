package com.interviewhelper.interview;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.interviewhelper.common.BusinessException;
import com.interviewhelper.interview.InterviewResponses.FeedbackCategoryResponse;
import com.interviewhelper.interview.InterviewResponses.QuestionResultResponse;
import com.interviewhelper.resume.ResumeData;
import com.interviewhelper.resume.ResumeService;

@Service
public class InterviewService {

	private static final int DEFAULT_QUESTION_COUNT = 5;
	private static final int MAX_QUESTION_COUNT = 10;

	private final ResumeService resumeService;
	private final AtomicLong interviewSequence = new AtomicLong(10);
	private final AtomicLong questionSequence = new AtomicLong(101);
	private final AtomicLong answerSequence = new AtomicLong(1001);
	private final Map<Long, InterviewData> interviews = new ConcurrentHashMap<>();
	private final Map<Long, AnswerData> answers = new ConcurrentHashMap<>();
	private final Map<Long, FeedbackData> feedbacks = new ConcurrentHashMap<>();

	public InterviewService(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	public InterviewData createInterview(Long resumeId, Integer questionCount) {
		ResumeData resume = resumeService.getResume(resumeId);
		int count = normalizeQuestionCount(questionCount);
		List<QuestionData> questions = createQuestions(resume, count);

		InterviewData interview = new InterviewData(
			interviewSequence.getAndIncrement(),
			resume.resumeId(),
			resume.jobRole(),
			questions,
			LocalDateTime.now()
		);
		interviews.put(interview.interviewId(), interview);
		return interview;
	}

	public List<QuestionData> getQuestions(Long interviewId) {
		return getInterview(interviewId).questions();
	}

	public AnswerData submitAnswer(Long interviewId, Long questionId, String answerText, Integer durationSeconds) {
		return saveAnswer(interviewId, questionId, answerText, durationSeconds, null, null);
	}

	public AnswerData submitAnswerAnalysis(
		Long interviewId,
		Long questionId,
		String answerText,
		Integer durationSeconds,
		EyeAnalysis eyeAnalysis,
		SpeechAnalysis speechAnalysis
	) {
		return saveAnswer(interviewId, questionId, answerText, durationSeconds, eyeAnalysis, speechAnalysis);
	}

	public FeedbackData createFeedback(Long interviewId, List<Long> answerIds) {
		InterviewData interview = getInterview(interviewId);
		List<AnswerData> targetAnswers = resolveAnswers(interviewId, answerIds);
		int contentScore = scoreContent(targetAnswers);
		int eyeScore = scoreEye(targetAnswers);
		int speechScore = scoreSpeech(targetAnswers);
		int totalScore = clamp((contentScore + eyeScore + speechScore) / 3);

		FeedbackCategoryResponse contentFeedback = new FeedbackCategoryResponse(
			contentScore,
			"답변에서 담당 역할과 구현 내용을 설명했습니다.",
			"성과, 수치, 문제 해결 과정을 함께 말하면 설득력이 높아집니다."
		);
		FeedbackCategoryResponse eyeFeedback = new FeedbackCategoryResponse(
			eyeScore,
			"대부분의 답변 흐름에서 화면 응시를 유지했습니다.",
			"핵심 문장을 말할 때 시선이 흔들리지 않도록 짧은 문장 단위로 연습해 보세요."
		);
		FeedbackCategoryResponse speechFeedback = new FeedbackCategoryResponse(
			speechScore,
			"말 속도와 음량 흐름은 면접 상황에 사용할 수 있는 수준입니다.",
			"습관어를 줄이고 문장 사이에 의도적인 쉼을 넣으면 더 안정적으로 들립니다."
		);
		List<QuestionResultResponse> questionResults = createQuestionResults(interview, targetAnswers, contentScore);

		FeedbackData feedback = new FeedbackData(
			interviewId,
			totalScore,
			createSummary(totalScore),
			contentFeedback,
			eyeFeedback,
			speechFeedback,
			createRecommendedAnswer(interview, targetAnswers),
			questionResults
		);
		feedbacks.put(interviewId, feedback);
		return feedback;
	}

	public FeedbackData getFeedback(Long interviewId) {
		FeedbackData feedback = feedbacks.get(interviewId);
		if (feedback == null) {
			throw new BusinessException("FEEDBACK_NOT_FOUND", "생성된 면접 피드백이 없습니다.", HttpStatus.NOT_FOUND);
		}
		return feedback;
	}

	private AnswerData saveAnswer(
		Long interviewId,
		Long questionId,
		String answerText,
		Integer durationSeconds,
		EyeAnalysis eyeAnalysis,
		SpeechAnalysis speechAnalysis
	) {
		InterviewData interview = getInterview(interviewId);
		validateQuestion(interview, questionId);

		if (answerText == null || answerText.isBlank()) {
			throw new BusinessException("ANSWER_TEXT_EMPTY", "답변 내용을 입력해 주세요.", HttpStatus.BAD_REQUEST);
		}

		AnswerData answer = new AnswerData(
			answerSequence.getAndIncrement(),
			interviewId,
			questionId,
			answerText.trim(),
			durationSeconds == null ? 0 : durationSeconds,
			eyeAnalysis,
			speechAnalysis,
			LocalDateTime.now()
		);
		answers.put(answer.answerId(), answer);
		return answer;
	}

	private InterviewData getInterview(Long interviewId) {
		InterviewData interview = interviews.get(interviewId);
		if (interview == null) {
			throw new BusinessException("INTERVIEW_NOT_FOUND", "면접을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
		}
		return interview;
	}

	private void validateQuestion(InterviewData interview, Long questionId) {
		boolean exists = interview.questions()
			.stream()
			.anyMatch(question -> question.questionId().equals(questionId));

		if (!exists) {
			throw new BusinessException("QUESTION_NOT_FOUND", "면접에 포함된 질문이 아닙니다.", HttpStatus.NOT_FOUND);
		}
	}

	private int normalizeQuestionCount(Integer questionCount) {
		if (questionCount == null) {
			return DEFAULT_QUESTION_COUNT;
		}
		return Math.min(Math.max(questionCount, 1), MAX_QUESTION_COUNT);
	}

	private List<QuestionData> createQuestions(ResumeData resume, int count) {
		List<QuestionTemplate> templates = List.of(
			new QuestionTemplate("PROJECT", "대표 프로젝트에서 본인이 담당한 핵심 기능은 무엇인가요?"),
			new QuestionTemplate("TECH", resume.jobRole() + " 업무에서 가장 자신 있는 기술과 그 이유는 무엇인가요?"),
			new QuestionTemplate("EXPERIENCE", "프로젝트 진행 중 가장 어려웠던 문제와 해결 방법을 설명해 주세요."),
			new QuestionTemplate("PROJECT", "이력서에 적은 경험 중 성과를 수치로 설명할 수 있는 사례는 무엇인가요?"),
			new QuestionTemplate("JOB_ROLE", resume.jobRole() + " 직무에 지원한 동기와 강점은 무엇인가요?"),
			new QuestionTemplate("TECH", "Spring Boot를 사용했다면 구조와 장점을 어떻게 설명하시겠어요?"),
			new QuestionTemplate("EXPERIENCE", "협업 과정에서 의견 충돌이 있었을 때 어떻게 해결했나요?"),
			new QuestionTemplate("PROJECT", "본인이 구현한 기능의 예외 상황과 테스트 방법을 설명해 주세요."),
			new QuestionTemplate("TECH", "데이터베이스 성능을 개선해야 한다면 어떤 순서로 접근하겠어요?"),
			new QuestionTemplate("EXPERIENCE", "최근 학습한 기술을 프로젝트에 적용한 경험이 있나요?")
		);

		List<QuestionData> questions = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			QuestionTemplate template = templates.get(index % templates.size());
			questions.add(new QuestionData(
				questionSequence.getAndIncrement(),
				index + 1,
				template.type(),
				template.content()
			));
		}
		return questions;
	}

	private List<AnswerData> resolveAnswers(Long interviewId, List<Long> answerIds) {
		List<AnswerData> resolved = new ArrayList<>();

		if (answerIds == null || answerIds.isEmpty()) {
			resolved.addAll(answers.values()
				.stream()
				.filter(answer -> answer.interviewId().equals(interviewId))
				.sorted(Comparator.comparing(AnswerData::createdAt))
				.toList());
		}
		else {
			for (Long answerId : answerIds) {
				AnswerData answer = answers.get(answerId);
				if (answer == null || !answer.interviewId().equals(interviewId)) {
					throw new BusinessException("ANSWER_NOT_FOUND", "면접에 포함된 답변을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
				}
				resolved.add(answer);
			}
		}

		if (resolved.isEmpty()) {
			throw new BusinessException("ANSWER_NOT_FOUND", "피드백을 생성할 답변이 없습니다.", HttpStatus.BAD_REQUEST);
		}

		return resolved;
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

	private List<QuestionResultResponse> createQuestionResults(InterviewData interview, List<AnswerData> targetAnswers, int contentScore) {
		return targetAnswers.stream()
			.map(answer -> {
				QuestionData question = interview.questions()
					.stream()
					.filter(candidate -> candidate.questionId().equals(answer.questionId()))
					.findFirst()
					.orElseThrow();
				return new QuestionResultResponse(
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

	private record QuestionTemplate(String type, String content) {
	}
}
