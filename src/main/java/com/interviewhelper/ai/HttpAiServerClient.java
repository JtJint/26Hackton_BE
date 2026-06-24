package com.interviewhelper.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewhelper.common.BusinessException;
import com.interviewhelper.interview.AnswerData;
import com.interviewhelper.interview.EyeAnalysis;
import com.interviewhelper.interview.InterviewData;
import com.interviewhelper.interview.QuestionData;
import com.interviewhelper.interview.SpeechAnalysis;
import com.interviewhelper.resume.ResumeData;

@Service
public class HttpAiServerClient implements AiServerClient {

	private static final Logger log = LoggerFactory.getLogger(HttpAiServerClient.class);

	private final String baseUrl;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public HttpAiServerClient(@Value("${ai.server.base-url}") String baseUrl) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		this.objectMapper = new ObjectMapper().findAndRegisterModules();
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.version(HttpClient.Version.HTTP_1_1)
			.build();
	}

	@Override
	public AiResumeAnalysisResult analyzeResume(ResumeData resume) {
		AnalyzeResumeResponse response = requestAnalyzeResume(resume, 5);

		return new AiResumeAnalysisResult(
			response.summary(),
			List.of(),
			List.of(),
			List.of(),
			List.of("AI 서버가 이력서 기반 예상 질문을 생성했습니다."),
			List.of("질문별 답변에서는 구체적인 역할, 행동, 결과를 함께 설명해 주세요."),
			response.questions().stream().map(QuestionItem::text).toList()
		);
	}

	@Override
	public List<AiQuestionResult> generateQuestions(ResumeData resume, int questionCount) {
		AnalyzeResumeResponse response = requestAnalyzeResume(resume, questionCount);

		return response.questions()
			.stream()
			.map(question -> new AiQuestionResult(
				question.id(),
				toQuestionType(question.category()),
				question.text(),
				blankToDefault(question.intent(), question.category()),
				blankToDefault(question.category(), "tech")
			))
			.toList();
	}

	@Override
	public AiTranscriptionResult transcribeAudio(String filename, String contentType, byte[] audioBytes) {
		TranscribeResponse response = postMultipartAudio(
			"/transcribe",
			filename,
			contentType,
			audioBytes,
			TranscribeResponse.class
		);

		return new AiTranscriptionResult(
			response.transcript(),
			response.durationSeconds() == null ? 0.0 : response.durationSeconds().doubleValue(),
			toPaceSpm(response.speechAnalysis()),
			response.paceStatus(),
			response.speechAnalysis() == null || response.speechAnalysis().fillerWordCount() == null
				? 0
				: response.speechAnalysis().fillerWordCount(),
			response.fillerWords()
				.stream()
				.map(word -> new AiTranscriptionResult.FillerWordResult(word.word(), word.count()))
				.toList()
		);
	}

	@Override
	public AiFeedbackResult generateFeedback(InterviewData interview, List<AnswerData> answers) {
		List<AiQuestionFeedbackResult> questionResults = new ArrayList<>();
		List<AiFeedbackCategoryResult> contentFeedbacks = new ArrayList<>();
		List<AiFeedbackCategoryResult> eyeFeedbacks = new ArrayList<>();
		List<AiFeedbackCategoryResult> speechFeedbacks = new ArrayList<>();
		List<String> followUps = new ArrayList<>();
		int totalScore = 0;

		for (AnswerData answer : answers) {
			QuestionData question = findQuestion(interview, answer.questionId());
			FeedbackResponse response = requestFeedback(interview, question, answer);
			totalScore += response.overallScore();
			contentFeedbacks.add(response.contentFeedback());
			eyeFeedbacks.add(response.eyeFeedback());
			speechFeedbacks.add(response.speechFeedback());
			followUps.addAll(response.followUpQuestions());
			questionResults.add(new AiQuestionFeedbackResult(
				question.questionId(),
				question.content(),
				answer.answerText(),
				response.overallScore(),
				toFeedbackText(response.contentFeedback())
			));
		}

		int averageScore = answers.isEmpty() ? 0 : Math.round((float) totalScore / answers.size());
		String recommendedAnswer = followUps.isEmpty()
			? "문제 상황, 본인의 역할, 해결 방법, 결과 순서로 답변을 다시 구성해 보세요."
			: "추가로 준비할 꼬리질문: " + String.join(" / ", followUps.stream().limit(2).toList());

		return new AiFeedbackResult(
			averageScore,
			"AI 서버가 답변 내용과 전달 지표를 종합해 피드백을 생성했습니다.",
			averageFeedback(contentFeedbacks, averageScore, "답변에서 핵심 경험을 설명했습니다.", "답변에 구체적인 근거와 결과를 더하면 좋습니다."),
			averageFeedback(eyeFeedbacks, averageScore, "시선 지표를 피드백에 반영했습니다.", "핵심 문장에서는 화면을 바라보며 말하는 연습을 해보세요."),
			averageFeedback(speechFeedbacks, averageScore, "발화 지표를 피드백에 반영했습니다.", "말 속도와 습관어를 점검하며 답변을 짧게 끊어 말해보세요."),
			recommendedAnswer,
			questionResults
		);
	}

	private AnalyzeResumeResponse requestAnalyzeResume(ResumeData resume, int questionCount) {
		AnalyzeResumeResponse response = postJson(
			"/analyze-resume",
			new AnalyzeResumeRequest(
				buildResumeText(resume),
				buildJobRole(resume),
				toAiCareerLevel(resume),
				resume.position().name(),
				resume.interviewType().name(),
				questionCount
			),
			AnalyzeResumeResponse.class
		);

		if (response == null || response.questions() == null) {
			throw new BusinessException("AI_SERVER_INVALID_RESPONSE", "AI 서버의 이력서 분석 응답이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
		}
		return response;
	}

	private FeedbackResponse requestFeedback(InterviewData interview, QuestionData question, AnswerData answer) {
		FeedbackResponse response = postJson(
			"/feedback",
			new FeedbackRequest(
				question.content(),
				blankToDefault(question.intent(), question.type()),
				answer.answerText(),
				FeedbackMetrics.from(answer.eyeAnalysis(), answer.speechAnalysis()),
				blankToDefault(question.category(), "tech"),
				interview.position().name(),
				toAiCareerLevel(interview)
			),
			FeedbackResponse.class
		);

		if (response == null) {
			throw new BusinessException("AI_SERVER_INVALID_RESPONSE", "AI 서버의 피드백 응답이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
		}
		return response;
	}

	private <T> T postJson(String path, Object requestBody, Class<T> responseType) {
		try {
			String json = objectMapper.writeValueAsString(requestBody);
			HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
				.version(HttpClient.Version.HTTP_1_1)
				.timeout(Duration.ofSeconds(60))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();
			HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);

			if (response.statusCode() >= 400) {
				log.warn("AI server request failed. path={}, status={}, body={}", path, response.statusCode(), response.body());
				throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
			}

			return objectMapper.readValue(response.body(), responseType);
		}
		catch (IOException exception) {
			log.warn("AI server JSON request failed. path={}", path, exception);
			throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("AI server request interrupted. path={}", path, exception);
			throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 요청이 중단되었습니다.", HttpStatus.BAD_GATEWAY);
		}
	}

	private <T> T postMultipartAudio(String path, String filename, String contentType, byte[] audioBytes, Class<T> responseType) {
		String boundary = "----interview-helper-" + System.currentTimeMillis();
		String safeFilename = filename == null || filename.isBlank() ? "answer.webm" : filename;
		String safeContentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;

		try {
			List<byte[]> bodyParts = List.of(
				("--" + boundary + "\r\n"
					+ "Content-Disposition: form-data; name=\"audio\"; filename=\"" + safeFilename + "\"\r\n"
					+ "Content-Type: " + safeContentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8),
				audioBytes,
				("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8)
			);
			HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
				.version(HttpClient.Version.HTTP_1_1)
				.timeout(Duration.ofSeconds(120))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArrays(bodyParts))
				.build();
			HttpResponse<String> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
			);

			if (response.statusCode() >= 400) {
				log.warn("AI server multipart request failed. path={}, status={}, body={}", path, response.statusCode(), response.body());
				throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 음성 분석 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
			}

			return objectMapper.readValue(response.body(), responseType);
		}
		catch (IOException exception) {
			log.warn("AI server multipart request failed. path={}", path, exception);
			throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 음성 분석 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("AI server multipart request interrupted. path={}", path, exception);
			throw new BusinessException("AI_SERVER_REQUEST_FAILED", "AI 서버 음성 분석 요청이 중단되었습니다.", HttpStatus.BAD_GATEWAY);
		}
	}

	private QuestionData findQuestion(InterviewData interview, Long questionId) {
		return interview.questions()
			.stream()
			.filter(question -> Objects.equals(question.questionId(), questionId))
			.findFirst()
			.orElseThrow(() -> new BusinessException("QUESTION_NOT_FOUND", "면접에 포함된 질문이 아닙니다.", HttpStatus.NOT_FOUND));
	}

	private String buildResumeText(ResumeData resume) {
		return """
			[경력 수준] %s
			[포지션] %s
			[면접 유형] %s
			[이력서]
			%s
			""".formatted(
			resume.careerLevel(),
			resume.position(),
			resume.interviewType(),
			resume.extractedText()
		);
	}

	private String buildJobRole(ResumeData resume) {
		return "%s / %s / %s / %s".formatted(
			resume.jobRole(),
			resume.careerLevel(),
			resume.position(),
			resume.interviewType()
		);
	}

	private String buildJobRole(InterviewData interview) {
		return "%s / %s / %s / %s".formatted(
			interview.jobRole(),
			interview.careerLevel(),
			interview.position(),
			interview.interviewType()
		);
	}

	private String toAiCareerLevel(InterviewData interview) {
		return switch (interview.careerLevel()) {
			case JUNIOR, NEWCOMER -> "JUNIOR";
			case EXPERIENCED -> "EXPERIENCED";
		};
	}

	private String toAiCareerLevel(ResumeData resume) {
		return switch (resume.careerLevel()) {
			case JUNIOR, NEWCOMER -> "JUNIOR";
			case EXPERIENCED -> "EXPERIENCED";
		};
	}

	private String toQuestionType(String category) {
		String normalized = blankToDefault(category, "tech").toLowerCase();
		return switch (normalized) {
			case "behavioral", "personality" -> "PERSONALITY";
			case "algorithm" -> "ALGORITHM";
			default -> "TECH";
		};
	}

	private String blankToDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private AiFeedbackCategoryResult averageFeedback(
		List<AiFeedbackCategoryResult> feedbacks,
		int defaultScore,
		String defaultStrength,
		String defaultImprovement
	) {
		if (feedbacks.isEmpty()) {
			return new AiFeedbackCategoryResult(defaultScore, defaultStrength, defaultImprovement);
		}

		int score = Math.round((float) feedbacks.stream()
			.map(AiFeedbackCategoryResult::score)
			.filter(Objects::nonNull)
			.mapToInt(Integer::intValue)
			.average()
			.orElse(defaultScore));
		String strength = feedbacks.stream()
			.map(AiFeedbackCategoryResult::strength)
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse(defaultStrength);
		String improvement = feedbacks.stream()
			.map(AiFeedbackCategoryResult::improvement)
			.filter(value -> value != null && !value.isBlank())
			.findFirst()
			.orElse(defaultImprovement);

		return new AiFeedbackCategoryResult(score, strength, improvement);
	}

	private String toFeedbackText(AiFeedbackCategoryResult feedback) {
		if (feedback == null) {
			return "답변 내용을 기반으로 피드백을 생성했습니다.";
		}
		return "%s 개선점: %s".formatted(
			blankToDefault(feedback.strength(), "답변의 핵심을 설명했습니다."),
			blankToDefault(feedback.improvement(), "구체적인 근거를 더하면 좋습니다.")
		);
	}

	public record AnalyzeResumeRequest(
		String resumeText,
		String jobRole,
		String careerLevel,
		String position,
		String interviewType,
		Integer numQuestions
	) {
	}

	public record AnalyzeResumeResponse(
		String summary,
		List<QuestionItem> questions
	) {
	}

	public record QuestionItem(
		String id,
		String text,
		String intent,
		String category
	) {
	}

	public record FeedbackRequest(
		String question,
		@JsonProperty("question_intent") String questionIntent,
		@JsonProperty("answer_transcript") String answerTranscript,
		FeedbackMetrics metrics,
		String category,
		String position,
		String experience
	) {
	}

	public record FeedbackMetrics(
		@JsonProperty("pace_spm") Integer paceSpm,
		@JsonProperty("pace_status") String paceStatus,
		@JsonProperty("filler_total") Integer fillerTotal,
		Double screenFocusRatio,
		Integer gazeAwayCount,
		Integer headMovementScore
	) {
		static FeedbackMetrics from(EyeAnalysis eyeAnalysis, SpeechAnalysis speechAnalysis) {
			int paceSpm = toPaceSpm(speechAnalysis);
			return new FeedbackMetrics(
				paceSpm,
				toPaceStatus(paceSpm),
				speechAnalysis == null || speechAnalysis.fillerWordCount() == null ? 0 : speechAnalysis.fillerWordCount(),
				eyeAnalysis == null || eyeAnalysis.screenFocusRatio() == null ? 0.7 : eyeAnalysis.screenFocusRatio(),
				eyeAnalysis == null || eyeAnalysis.gazeAwayCount() == null ? 0 : eyeAnalysis.gazeAwayCount(),
				eyeAnalysis == null || eyeAnalysis.headMovementScore() == null ? 75 : eyeAnalysis.headMovementScore()
			);
		}

		private static int toPaceSpm(SpeechAnalysis speechAnalysis) {
			if (speechAnalysis == null || speechAnalysis.wordsPerMinute() == null) {
				return 320;
			}
			return Math.max(0, (int) Math.round(speechAnalysis.wordsPerMinute() * 2.5));
		}

		private static String toPaceStatus(int paceSpm) {
			if (paceSpm < 300) {
				return "slow";
			}
			if (paceSpm > 350) {
				return "fast";
			}
			return "ideal";
		}
	}

	public record FeedbackResponse(
		@JsonProperty("content_feedback") AiFeedbackCategoryResult contentFeedback,
		@JsonProperty("eye_feedback") AiFeedbackCategoryResult eyeFeedback,
		@JsonProperty("speech_feedback") AiFeedbackCategoryResult speechFeedback,
		@JsonProperty("overall_score") Integer overallScore,
		@JsonProperty("recommended_answer") String recommendedAnswer,
		@JsonProperty("follow_up_questions") List<String> followUpQuestions
	) {
		public FeedbackResponse {
			contentFeedback = defaultFeedback(
				contentFeedback,
				overallScore,
				"답변에서 핵심 경험을 설명했습니다.",
				"답변에 구체적인 근거와 결과를 더하면 좋습니다."
			);
			eyeFeedback = defaultFeedback(
				eyeFeedback,
				overallScore,
				"시선 지표를 피드백에 반영했습니다.",
				"핵심 문장에서는 화면을 바라보며 말하는 연습을 해보세요."
			);
			speechFeedback = defaultFeedback(
				speechFeedback,
				overallScore,
				"발화 지표를 피드백에 반영했습니다.",
				"말 속도와 습관어를 점검하며 답변을 짧게 끊어 말해보세요."
			);
			overallScore = overallScore == null ? 0 : overallScore;
			recommendedAnswer = recommendedAnswer == null ? "" : recommendedAnswer;
			followUpQuestions = followUpQuestions == null ? List.of() : followUpQuestions;
		}

		private static AiFeedbackCategoryResult defaultFeedback(
			AiFeedbackCategoryResult feedback,
			Integer score,
			String strength,
			String improvement
		) {
			if (feedback != null) {
				return feedback;
			}
			return new AiFeedbackCategoryResult(score == null ? 0 : score, strength, improvement);
		}
	}

	public record TranscribeResponse(
		String transcript,
		Integer durationSeconds,
		TranscribeSpeechAnalysis speechAnalysis,
		@JsonProperty("pace_status") String paceStatus,
		@JsonProperty("filler_words") List<FillerWord> fillerWords
	) {
		public TranscribeResponse {
			fillerWords = fillerWords == null ? List.of() : fillerWords;
		}
	}

	public record TranscribeSpeechAnalysis(
		Integer wordsPerMinute,
		Integer fillerWordCount,
		Double silenceSeconds,
		Integer volumeStabilityScore
	) {
	}

	private int toPaceSpm(TranscribeSpeechAnalysis speechAnalysis) {
		if (speechAnalysis == null || speechAnalysis.wordsPerMinute() == null) {
			return 320;
		}
		return Math.max(0, (int) Math.round(speechAnalysis.wordsPerMinute() * 2.5));
	}

	public record FillerWord(
		String word,
		Integer count
	) {
	}
}
