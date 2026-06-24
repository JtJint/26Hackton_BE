package com.interviewhelper.speech;

import org.springframework.stereotype.Service;

import com.interviewhelper.ai.AiServerClient;
import com.interviewhelper.ai.AiSpeechSynthesisResult;

@Service
public class SpeechService {

	private final AiServerClient aiServerClient;

	public SpeechService(AiServerClient aiServerClient) {
		this.aiServerClient = aiServerClient;
	}

	public AiSpeechSynthesisResult synthesize(String text, String voice, String instructions) {
		return aiServerClient.synthesizeSpeech(text, normalizeVoice(voice), blankToNull(instructions));
	}

	private String normalizeVoice(String voice) {
		return voice == null || voice.isBlank() ? "nova" : voice;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
