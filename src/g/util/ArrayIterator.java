package g.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread safe iterator over an array
 * @author Gergely Bálint
 *
 * @param <B>
 * @param <A>
 */
public class ArrayIterator<B,A extends B> extends AtomicInteger implements Iterator<B>,Enumeration<B>{
	private static final long serialVersionUID = 1L;
	protected final A[] array;
	public ArrayIterator(A[] array0) {
		array = array0;
	}
	@Override
	public boolean hasMoreElements() {
		return hasNext();
	}
	@Override
	public B nextElement() {
		return next();
	}
	@Override
	public boolean hasNext() {
		int i = get();
		return i >= 0 && i < array.length;
	}
	@Override
	public B next() {
		int index = super.getAndUpdate((int i) -> i >= array.length || i < 0 ? i : i+1);
		if(index >= array.length || index < 0){
			throw new NoSuchElementException();
		}
		return array[index];
	}
}
