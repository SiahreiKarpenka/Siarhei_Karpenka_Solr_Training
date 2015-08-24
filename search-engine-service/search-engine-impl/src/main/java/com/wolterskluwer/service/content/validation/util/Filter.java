package com.wolterskluwer.service.content.validation.util;

public interface Filter<T> {
	public boolean match(T item);
}
