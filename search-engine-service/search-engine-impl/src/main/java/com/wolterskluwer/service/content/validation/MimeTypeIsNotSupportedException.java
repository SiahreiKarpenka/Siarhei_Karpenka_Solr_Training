package com.wolterskluwer.service.content.validation;

public class MimeTypeIsNotSupportedException extends Exception {

	private static final long serialVersionUID = 1L;

	public MimeTypeIsNotSupportedException() {
		super();
	}

	public MimeTypeIsNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public MimeTypeIsNotSupportedException(String message) {
		super(message);
	}

	public MimeTypeIsNotSupportedException(Throwable cause) {
		super(cause);
	}

}
