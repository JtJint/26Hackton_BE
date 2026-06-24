package com.interviewhelper.speech;

import com.interviewhelper.ai.AiSpeechSynthesisResult;

public final class SpeechResponses {

	private SpeechResponses() {
	}

	public record SynthesizeSpeechResponse(
		String text,
		String audioBase64,
		String contentType,
		String format,
		String voice
	) {
		public static SynthesizeSpeechResponse from(AiSpeechSynthesisResult result) {
			return new SynthesizeSpeechResponse(
				result.text(),
				result.audioBase64(),
				result.contentType(),
				result.format(),
				result.voice()
			);
		}
	}
}
