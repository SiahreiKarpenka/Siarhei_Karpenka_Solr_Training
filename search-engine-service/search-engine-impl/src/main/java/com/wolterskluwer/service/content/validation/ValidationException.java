package com.wolterskluwer.service.content.validation;

public class ValidationException extends Exception {
	private static final long serialVersionUID = -984129932203989511L;

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
