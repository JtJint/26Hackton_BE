package com.interviewhelper.interview;

import java.time.LocalDateTime;
import java.util.List;

import com.interviewhelper.resume.CareerLevel;
import com.interviewhelper.resume.InterviewType;
import com.interviewhelper.resume.Position;

public final class InterviewResponses {

	private InterviewResponses() {
	}

	public record QuestionResponse(
		Long questionId,
		Integer order,
		String type,
		String content,
		String intent,
		String category
	) {
		public static QuestionResponse from(QuestionData question) {
			return new QuestionResponse(
				question.questionId(),
				question.order(),
				question.type(),
				question.content(),
				question.intent(),
				question.category()
			);
		}
	}

	public record InterviewResponse(
		Long interviewId,
		Long resumeId,
		String jobRole,
		CareerLevel careerLevel,
		Position position,
		InterviewType interviewType,
		List<QuestionResponse> questions,
		LocalDateTime createdAt
	) {
		public static InterviewResponse from(InterviewData interview) {
			return new InterviewResponse(
				interview.interviewId(),
				interview.resumeId(),
				interview.jobRole(),
				interview.careerLevel(),
				interview.position(),
				interview.interviewType(),
				interview.questions().stream().map(QuestionResponse::from).toList(),
				interview.createdAt()
			);
		}
	}

	public record QuestionsResponse(
		Long interviewId,
		List<QuestionResponse> questions
	) {
	}

	public record AnswerResponse(
		Long answerId,
		Long questionId,
		Boolean saved,
		LocalDateTime createdAt
	) {
		public static AnswerResponse from(AnswerData answer) {
			return new AnswerResponse(answer.answerId(), answer.questionId(), true, answer.createdAt());
		}
	}

	public record TranscribedAnswerResponse(
		Long answerId,
		Long questionId,
		Boolean saved,
		String transcript,
		Integer durationSeconds,
		SpeechAnalysis speechAnalysis,
		LocalDateTime createdAt
	) {
		public static TranscribedAnswerResponse from(AnswerData answer) {
			return new TranscribedAnswerResponse(
				answer.answerId(),
				answer.questionId(),
				true,
				answer.answerText(),
				answer.durationSeconds(),
				answer.speechAnalysis(),
				answer.createdAt()
			);
		}
	}

	public record FeedbackCategoryResponse(
		Integer score,
		String strength,
		String improvement
	) {
	}

	public record FeedbackResponse(
		Long interviewId,
		Integer totalScore,
		String summary,
		FeedbackCategoryResponse contentFeedback,
		FeedbackCategoryResponse eyeFeedback,
		FeedbackCategoryResponse speechFeedback,
		String recommendedAnswer
	) {
		public static FeedbackResponse from(FeedbackData feedback) {
			return new FeedbackResponse(
				feedback.interviewId(),
				feedback.totalScore(),
				feedback.summary(),
				feedback.contentFeedback(),
				feedback.eyeFeedback(),
				feedback.speechFeedback(),
				feedback.recommendedAnswer()
			);
		}
	}

	public record QuestionResultResponse(
		Long questionId,
		String question,
		String answer,
		Integer score,
		String feedback
	) {
	}

	public record FeedbackResultResponse(
		Long interviewId,
		Integer totalScore,
		String summary,
		List<QuestionResultResponse> questionResults
	) {
		public static FeedbackResultResponse from(FeedbackData feedback) {
			return new FeedbackResultResponse(
				feedback.interviewId(),
				feedback.totalScore(),
				feedback.summary(),
				feedback.questionResults()
			);
		}
	}
}
