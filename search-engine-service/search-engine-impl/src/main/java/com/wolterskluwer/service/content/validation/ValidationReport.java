package com.wolterskluwer.service.content.validation;

import java.util.List;

import com.wolterskluwer.service.content.validation.util.Message;

public interface ValidationReport {
	List<Message> getMessages();

}