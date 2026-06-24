package com.interviewhelper.speech;

import org.springframework.stereotype.Service;

import com.interviewhelper.ai.AiServerClient;
import com.interviewhelper.ai.AiSpeechSynthesisResult;
import com.interviewhelper.resume.InterviewerType;

@Service
public class SpeechService {

	private final AiServerClient aiServerClient;

	public SpeechService(AiServerClient aiServerClient) {
		this.aiServerClient = aiServerClient;
	}

	public AiSpeechSynthesisResult synthesize(String text, String voice, InterviewerType interviewerType, String instructions) {
		return aiServerClient.synthesizeSpeech(
			text,
			normalizeVoice(voice),
			interviewerType == null ? InterviewerType.SOFT : interviewerType,
			blankToNull(instructions)
		);
	}

	private String normalizeVoice(String voice) {
		return voice == null || voice.isBlank() ? "nova" : voice;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
