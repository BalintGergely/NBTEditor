package net.balintgergely.nbt;

public class Link<E>{
	public E element;
	public Link<E> next;
	public Link(){}
	public Link(E element,Link<E> next){
		this.element = element;
		this.next = next;
	}
}
