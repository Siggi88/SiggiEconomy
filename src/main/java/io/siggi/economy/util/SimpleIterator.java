package io.siggi.economy.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class SimpleIterator<T> implements Iterator<T> {

	protected abstract T getNext();

	private T next = null;
	private boolean nextWasDetermined = false;

	private void determineNext() {
		if (nextWasDetermined) {
			return;
		}
		next = getNext();
		nextWasDetermined = true;
	}

	@Override
	public final boolean hasNext() {
		determineNext();
		return next != null;
	}

	@Override
	public final T next() {
		determineNext();
		if (next == null) {
			throw new NoSuchElementException();
		}
		try {
			return next;
		} finally {
			nextWasDetermined = false;
			next = null;
		}
	}

}
