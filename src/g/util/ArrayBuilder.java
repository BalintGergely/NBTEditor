package g.util;

import java.lang.reflect.Array;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ArrayBuilder<E> implements Consumer<E>,Supplier<E[]>{
	public final Class<E> type;
	private Link<E> first,current;
	private int size;
	public ArrayBuilder(Class<E> type0){
		type = type0;
		first = current = new Link<>(null,null);
	}
	public void accept(E t) {
		if(type != null){
			type.cast(t);
		}
		current.element = t;
		current = current.next = new Link<>(null,null);
		size++;
	}
	@SuppressWarnings("unchecked")
	public E[] get() {
		E[] array = type == null ? (E[])new Object[size] : (E[])Array.newInstance(type, size);
		Link<E> e = first;
		int index = 0;
		while(e != null &&e != current){
			array[index] = e.element;
			index++;
			e = e.next;
		}
		return array;
	}
	public int size(){
		return size;
	}
}
