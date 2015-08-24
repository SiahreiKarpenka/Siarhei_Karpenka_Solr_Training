package com.wolterskluwer.service.content.validation;

import java.util.List;

import com.wolterskluwer.service.content.validation.util.Message;

public class ContentObjectValidationResult {

	private String sourceId;

	private List<Message> messages;

	/**
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * @param sourceId
	 *            the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
}
