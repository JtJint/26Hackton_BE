package com.interviewhelper.common;

public record ApiError(
	String code,
	String message
) {
}
