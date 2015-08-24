package com.wolterskluwer.service.content.validation.util;

public class Message {

	private String text;
	private String subject;
	private MessagePriority status;

	public Message(String text, String subject, MessagePriority status) {
		super();
		this.text = text;
		this.subject = subject;
		this.status = status;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @param subject
	 *            the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * @return the status
	 */
	public MessagePriority getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(MessagePriority status) {
		this.status = status;
	}

}
