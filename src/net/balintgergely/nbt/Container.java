package net.balintgergely.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

public abstract class Container<T extends Tag<?>> extends ArrayList<T>{
	private static final long serialVersionUID = 1L;
	final void iae(T val) throws IllegalArgumentException{
		if(!accept(val)){
			throw new IllegalArgumentException();
		}
	}
	Container(){
	}
	public abstract boolean accept(T val);
	public void replaceAll(Function<? super T, ? extends T> operator) {
		super.replaceAll((T t) -> {
			t = operator.apply(t);
			iae(t);
			return t;
		});
	}
	@Override
	public boolean add(T e){
		iae(e);
		return super.add(e);
	}
	@Override
	public boolean addAll(Collection<? extends T> col){
		super.ensureCapacity(super.size()+col.size());
		boolean wasChange = false;
		for(T t : col){
			wasChange = add(t) | wasChange;
		}
		return wasChange;
	}
	@Override
	public boolean addAll(int index,Collection<? extends T> col){
		super.ensureCapacity(super.size()+col.size());
		boolean wasChange = false;
		for(T t : col){
			add(index,t);
			index++;
			wasChange = true;
		}
		return wasChange;
	}
	@Override
	public T set(int index, T e) {
		iae(e);
		return super.set(index, e);
	}
	@Override
	public void add(int index, T e) {
		iae(e);
		super.add(index, e);
	}
	@Override
	@SuppressWarnings("unchecked")
	public Container<T> clone(){
		return (Container<T>)super.clone();
	}
}
