package g.util;
/**
 * A link is a container class for an element type, and another link of the same type.
 * @author Gergely Bálint
 *
 * @param <E>
 */
public class Link<E> {
	public E element;
	public Link<E> next;
	public Link(E element0,Link<E> next0) {
		element = element0;
		next = next0;
	}
}
