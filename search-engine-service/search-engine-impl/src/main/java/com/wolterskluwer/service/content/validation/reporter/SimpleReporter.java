package com.wolterskluwer.service.content.validation.reporter;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.wolterskluwer.service.content.validation.util.Filter;
import com.wolterskluwer.service.content.validation.util.ListUtils;
import com.wolterskluwer.service.content.validation.util.Message;
import com.wolterskluwer.service.content.validation.util.MessagePriority;

public class SimpleReporter implements UpdatableReporter {

	private final List<Message> messages;
	
    public SimpleReporter() {
    	messages = new ArrayList<Message>();
    }
    
    public List<Message> getMessages() {
		return messages;
	}

    @Override
    public void error(String text) {
    	addMessage(text, null, MessagePriority.ERROR);
    }

	protected void addMessage(String text, String subject, MessagePriority priority) {
		messages.add(new Message(text, subject, priority));
	}

    @Override
    public void warn(String text) {
    	addMessage(text, null, MessagePriority.WARNING);
    }

    @Override
    public void info(String text) {
    	addMessage(text, null, MessagePriority.INFO);
    }

    public List<String> getErrors() {
        return getMessagesTextByPriority(MessagePriority.ERROR);
    }

    @Override
    public Reporter getResourceReporter(String path) {
        return new FileReporter(path);
    }

    public List<String> getWarns() {
        return getMessagesTextByPriority(MessagePriority.WARNING);
    }

	private List<String> getMessagesTextByPriority(
			final MessagePriority priority) {
		List<Message> result = getMessagesByPriority(priority);
        List<String> messages = toStringList(result);
		return messages;
	}

	private List<Message> getMessagesByPriority(final MessagePriority priority) {
		List<Message> result = ListUtils.getFilteredList(messages, new Filter<Message>() {

			@Override
			public boolean match(Message item) {
				// TODO Auto-generated method stub
				return item.getStatus()!=null&&item.getStatus()==priority;
			}
		});
		return result;
	}

	private List<String> toStringList(List<Message> result) {
		List<String> messages = Lists.transform(result, new Function<Message, String>(){

			@Override
			public String apply(Message input) {
				return input.getText();
			}
        	
        });
		return messages;
	}

    public List<String> getInfos() {
    	return getMessagesTextByPriority(MessagePriority.INFO);
    }

    public List<String> getStarts() {
    	return getMessagesTextByPriority(MessagePriority.START);
    }

    public List<String> getCompletes() {
    	return getMessagesTextByPriority(MessagePriority.COMPLETE);
    }

    @Override
    public void start(String text) {
    	addMessage(text, null, MessagePriority.START);

    }

    @Override
    public void complete(String text) {
    	addMessage(text, null, MessagePriority.COMPLETE);
    }

    @Override
    public void setParameter(String name, Object value) {
        // do nothing here
    }

    private class FileReporter implements UpdatableReporter {

        private final String path;

        FileReporter(String path) {
            this.path = path;
        }

        @Override
        public void error(String message) {
            SimpleReporter.this.addMessage(message, path, MessagePriority.ERROR);
        }

        @Override
        public void warn(String message) {
            SimpleReporter.this.addMessage(message, path, MessagePriority.WARNING);
        }

        @Override
        public void info(String message) {
            SimpleReporter.this.addMessage(message, path, MessagePriority.INFO);
        }

        @Override
        public List<String> getErrors() {
            return SimpleReporter.this.getErrors();
        }

        @Override
        public Reporter getResourceReporter(String path) {
            return this;
        }

//        private String composeMessage(String message) {
//            StringBuilder sb = new StringBuilder(path);
//            sb.append(": ");
//            sb.append(message);
//            return sb.toString();
//        }

        @Override
        public void start(String message) {
            SimpleReporter.this.addMessage(message, path, MessagePriority.START);

        }

        @Override
        public void complete(String message) {
            SimpleReporter.this.addMessage(message, path, MessagePriority.COMPLETE);
        }

        @Override
        public void setParameter(String name, Object value) {
            // do nothing here
        }

        @Override
        public List<String> getWarnings() {
            return SimpleReporter.this.getWarnings();
        }

        @Override
        public void destroy() {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public List<String> getWarnings() {
    	return getMessagesTextByPriority(MessagePriority.WARNING);
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
