package g.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
/**
 * The MultiEntryQueue is a Linked Queue implementation with multiple end points, and the ability to insert an element
 * at any of the end points.
 * @author Gergely Bálint
 *
 * @param <E>
 */
public class MultiEntryQueue<E> implements Queue<E>{
	private Link<E> front;
	/**
	 * Every element guaranteed to be part of the collection, the last non-null element is the tail of the queue.
	 */
	private Link<E>[] endPoints;
	private int size;
	private int currentPoint;
	@SuppressWarnings("unchecked")
	public MultiEntryQueue(int endPoints0) {
		endPoints = (Link<E>[])new Link<?>[endPoints0];
	}
	public void setEndPoint(int endPoint){
		if(endPoint < 0 || endPoint >= endPoints.length){
			throw new IllegalArgumentException();
		}
		currentPoint = endPoint;
	}
	public int getEndPoint(){
		return currentPoint;
	}
	public int getNumberOfEndPoints(){
		return endPoints.length;
	}
	@Override
	public int size() {
		return size;
	}
	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	@Override
	public boolean contains(Object o) {
		Link<E> itr = front;
		while(itr != null){
			if(o.equals(itr.element)){
				return true;
			}
			itr = itr.next;
		}
		return false;
	}
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>(){
			private Link<E> a = front,b,c;
			@Override
			public boolean hasNext() {
				return a != null;
			}
			@Override
			public E next() {
				if(a == null){
					throw new NoSuchElementException();
				}
				c = b;
				b = a;
				a = a.next;
				return b.element;
			}
			@Override
			public void remove() {
				if(c == null){
					if(b == front){
						front = a;
						b.element = null;
						int i = 0;
						while(i < endPoints.length){
							if(endPoints[i] != null){
								if(endPoints[i] == b){
									endPoints[i] = null;
								}else{
									break;
								}
							}
							i++;
						}
						b = null;
						size--;
						return;
					}
					throw new IllegalStateException();
				}
				c.next = a;
				int i = 0;
				while(i < endPoints.length){
					if(endPoints[i] == b){
						endPoints[i] = null;
					}
					i++;
				}
				size--;
				b.element = null;
				b = c;
				c = null;
			}
		};
	}
	@Override
	public Object[] toArray() {
		Object[] o = new Object[size];
		Link<E> l = front;
		int i = 0;
		while(i < size){
			o[i] = l.element;
			l = l.next;
			i++;
		}
		return o;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length < size){
			a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
		}else if(a.length > size){
			a[size] = null;
		}
		Link<E> l = front;
		int i = 0;
		while(i < size){
			a[i] = (T)l.element;
			l = l.next;
			i++;
		}
		return a;
	}
	@Override
	public boolean remove(Object o) {
		if(front == null){
			return false;
		}
		if(o.equals(front.element)){
			front = front.next;
			size--;
			return true;
		}
		Link<E> l = front;
		while(l.next != null){
			if(o.equals(l.next.element)){
				l.next = l.next.next;
				size--;
				return true;
			}
			l = l.next;
		}
		return false;
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c){
			if(!contains(o)){
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean addAll(Collection<? extends E> c) {
		for(E e : c){
			add(e);
		}
		return true;
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return removeIf((Object o) -> c.contains(o));
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		return removeIf((Object o) -> !c.contains(o));
	}
	@Override
	public void clear() {
		int i = 0;
		while(i < endPoints.length){
			endPoints[i] = null;
			i++;
		}
		front = null;
	}
	@Override
	public boolean add(E e) {
		if(!offer(e)){
			throw new IllegalStateException();
		}
		return true;
	}
	@Override
	public boolean offer(E e) {
		Objects.requireNonNull(e);
		if(size == Integer.MAX_VALUE){
			return false;
		}
		int i = currentPoint;
		if(size == 0){
			front = endPoints[i] = new Link<>(e,null);
			size++;
			return true;
		}
		while(i > 0 && endPoints[i] == null){
			i--;
		}
		Link<E> prew = endPoints[i],c;
		if(prew == null){
			prew = front;
		}
		c = endPoints[currentPoint] = new Link<>(e,prew.next);
		prew.next = c;
		size++;
		return true;
	}
	@Override
	public E remove() {
		E e = poll();
		if(e == null){
			throw new NoSuchElementException();
		}
		return e;
	}
	@Override
	public E poll() {
		if(front == null){
			return null;
		}
		Link<E> current = front;
		front = front.next;
		int i = 0;
		while(i < endPoints.length){
			if(endPoints[i] != null){
				if(endPoints[i] == current){
					endPoints[i] = null;
				}else{
					break;
				}
			}
			i++;
		}
		size--;
		return current.element;
	}
	@Override
	public E element() {
		E e = peek();
		if(e == null){
			throw new NoSuchElementException();
		}
		return e;
	}
	@Override
	public E peek() {
		if(front == null){
			return null;
		}
		return front.element;
	}

}
