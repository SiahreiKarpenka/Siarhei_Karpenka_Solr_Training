package com.wolterskluwer.service.content.validation.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

	public static <T> List<T> getFilteredList(List<T> oldList, Filter<T> filter) {
		List<T> newlist = new ArrayList<T>();

		for (T item : oldList) {
			if (filter.match(item)) {
				newlist.add(item);
			}
		}

		return newlist;
	}
}
