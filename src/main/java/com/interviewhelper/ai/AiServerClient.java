package com.interviewhelper.ai;

import java.util.List;

import com.interviewhelper.interview.AnswerData;
import com.interviewhelper.interview.InterviewData;
import com.interviewhelper.resume.ResumeData;

public interface AiServerClient {

	AiResumeAnalysisResult analyzeResume(ResumeData resume);

	List<AiQuestionResult> generateQuestions(ResumeData resume, int questionCount);

	AiTranscriptionResult transcribeAudio(String filename, String contentType, byte[] audioBytes);

	AiFeedbackResult generateFeedback(InterviewData interview, List<AnswerData> answers);
}
