package com.interviewhelper.interview;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.interviewhelper.ai.AiFeedbackResult;
import com.interviewhelper.ai.AiQuestionResult;
import com.interviewhelper.ai.AiServerClient;
import com.interviewhelper.ai.AiTranscriptionResult;
import com.interviewhelper.common.BusinessException;
import com.interviewhelper.interview.InterviewResponses.FeedbackCategoryResponse;
import com.interviewhelper.interview.InterviewResponses.QuestionResultResponse;
import com.interviewhelper.resume.ResumeData;
import com.interviewhelper.resume.ResumeService;

@Service
public class InterviewService {

	private static final Logger log = LoggerFactory.getLogger(InterviewService.class);
	private static final int DEFAULT_QUESTION_COUNT = 5;
	private static final int MAX_QUESTION_COUNT = 10;

	private final ResumeService resumeService;
	private final AiServerClient aiServerClient;
	private final AtomicLong interviewSequence = new AtomicLong(10);
	private final AtomicLong questionSequence = new AtomicLong(101);
	private final AtomicLong answerSequence = new AtomicLong(1001);
	private final Map<Long, InterviewData> interviews = new ConcurrentHashMap<>();
	private final Map<Long, AnswerData> answers = new ConcurrentHashMap<>();
	private final Map<Long, FeedbackData> feedbacks = new ConcurrentHashMap<>();

	public InterviewService(ResumeService resumeService, AiServerClient aiServerClient) {
		this.resumeService = resumeService;
		this.aiServerClient = aiServerClient;
	}

	public InterviewData createInterview(Long resumeId, Integer questionCount) {
		ResumeData resume = resumeService.getResume(resumeId);
		int count = normalizeQuestionCount(questionCount);
		List<QuestionData> questions = createQuestions(resume, count);

		InterviewData interview = new InterviewData(
			interviewSequence.getAndIncrement(),
			resume.resumeId(),
			resume.jobRole(),
			resume.careerLevel(),
			resume.position(),
			resume.interviewType(),
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

	public AnswerData submitAudioAnswer(
		Long interviewId,
		Long questionId,
		MultipartFile audio,
		EyeAnalysis eyeAnalysis
	) {
		if (audio == null || audio.isEmpty()) {
			throw new BusinessException("AUDIO_FILE_EMPTY", "답변 음성 파일을 첨부해 주세요.", HttpStatus.BAD_REQUEST);
		}

		long startedAt = System.nanoTime();
		log.info(
			"Audio answer upload started. interviewId={}, questionId={}, filename={}, contentType={}, sizeBytes={}",
			interviewId,
			questionId,
			audio.getOriginalFilename(),
			audio.getContentType(),
			audio.getSize()
		);

		try {
			long readStartedAt = System.nanoTime();
			byte[] audioBytes = audio.getBytes();
			long readElapsedMs = elapsedMillis(readStartedAt);
			log.info(
				"Audio file read completed. interviewId={}, questionId={}, sizeBytes={}, elapsedMs={}",
				interviewId,
				questionId,
				audioBytes.length,
				readElapsedMs
			);

			long transcribeStartedAt = System.nanoTime();
			AiTranscriptionResult transcription = aiServerClient.transcribeAudio(
				audio.getOriginalFilename(),
				audio.getContentType(),
				audioBytes
			);
			long transcribeElapsedMs = elapsedMillis(transcribeStartedAt);
			log.info(
				"Audio transcription completed. interviewId={}, questionId={}, elapsedMs={}, transcriptLength={}, durationSeconds={}, paceSpm={}, fillerTotal={}",
				interviewId,
				questionId,
				transcribeElapsedMs,
				transcription.transcript() == null ? 0 : transcription.transcript().length(),
				transcription.durationSeconds(),
				transcription.paceSpm(),
				transcription.fillerTotal()
			);

			AnswerData answer = saveAnswer(
				interviewId,
				questionId,
				transcription.transcript(),
				toDurationSeconds(transcription.durationSeconds()),
				eyeAnalysis,
				toSpeechAnalysis(transcription)
			);
			log.info(
				"Audio answer upload completed. interviewId={}, questionId={}, answerId={}, totalElapsedMs={}",
				interviewId,
				questionId,
				answer.answerId(),
				elapsedMillis(startedAt)
			);
			return answer;
		}
		catch (java.io.IOException exception) {
			log.warn(
				"Audio file read failed. interviewId={}, questionId={}, filename={}, elapsedMs={}",
				interviewId,
				questionId,
				audio.getOriginalFilename(),
				elapsedMillis(startedAt),
				exception
			);
			throw new BusinessException("AUDIO_FILE_READ_FAILED", "답변 음성 파일을 읽지 못했습니다.", HttpStatus.BAD_REQUEST);
		}
	}

	public FeedbackData createFeedback(Long interviewId, List<Long> answerIds) {
		InterviewData interview = getInterview(interviewId);
		List<AnswerData> targetAnswers = resolveAnswers(interviewId, answerIds);
		AiFeedbackResult aiFeedback = aiServerClient.generateFeedback(interview, targetAnswers);

		FeedbackData feedback = new FeedbackData(
			interviewId,
			aiFeedback.totalScore(),
			aiFeedback.summary(),
			new FeedbackCategoryResponse(
				aiFeedback.contentFeedback().score(),
				aiFeedback.contentFeedback().strength(),
				aiFeedback.contentFeedback().improvement()
			),
			new FeedbackCategoryResponse(
				aiFeedback.eyeFeedback().score(),
				aiFeedback.eyeFeedback().strength(),
				aiFeedback.eyeFeedback().improvement()
			),
			new FeedbackCategoryResponse(
				aiFeedback.speechFeedback().score(),
				aiFeedback.speechFeedback().strength(),
				aiFeedback.speechFeedback().improvement()
			),
			aiFeedback.recommendedAnswer(),
			aiFeedback.questionResults()
				.stream()
				.map(result -> new QuestionResultResponse(
					result.questionId(),
					result.question(),
					result.answer(),
					result.score(),
					result.feedback()
				))
				.toList()
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
		List<AiQuestionResult> aiQuestions = aiServerClient.generateQuestions(resume, count);
		List<QuestionData> questions = new ArrayList<>();
		for (int index = 0; index < aiQuestions.size(); index++) {
			AiQuestionResult question = aiQuestions.get(index);
			questions.add(new QuestionData(
				questionSequence.getAndIncrement(),
				index + 1,
				question.type(),
				question.content(),
				question.intent(),
				question.category()
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

	private Integer toDurationSeconds(Double durationSeconds) {
		if (durationSeconds == null) {
			return 0;
		}
		return Math.max(0, (int) Math.round(durationSeconds));
	}

	private SpeechAnalysis toSpeechAnalysis(AiTranscriptionResult transcription) {
		int paceSpm = transcription.paceSpm() == null ? 320 : transcription.paceSpm();
		int wordsPerMinute = Math.max(0, (int) Math.round(paceSpm / 2.5));
		return new SpeechAnalysis(
			wordsPerMinute,
			transcription.fillerTotal() == null ? 0 : transcription.fillerTotal(),
			0.0,
			75
		);
	}

	private long elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000;
	}
}
