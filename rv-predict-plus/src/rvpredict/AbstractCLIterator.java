package rvpredict;

import java.util.List;
import java.util.ListIterator;
import rvpredict.util.ActuallyCloneable;

class AbstractCLIterator<E, L extends List<E>> implements CLIterator<E> {
  public final L list;
  public int index = 0;
  @Override public void add(final E e) { assert false: "Unimplemented"; }
  @Override public void set(final E e) { assert false: "Unimplemented"; }
  @Override public void remove() { assert false: "Unimplemented"; }
  @Override public boolean hasNext() { return index < list.size(); }
  @Override public boolean hasPrevious() { return 0 < index; }
  @Override public int nextIndex() { return index; }
  @Override public int previousIndex() { return index-1; }
  @Override public E next() { final E e = list.get(index); index++; return e; }
  @Override public E previous() { index--; return list.get(index); }
  @Override public AbstractCLIterator<E,L> clone() { return new AbstractCLIterator<E,L>(this); }
  AbstractCLIterator(final L l,final int i) { list = l; index = i; }
  AbstractCLIterator(final AbstractCLIterator<? extends E,? extends L> cli) { this.list = cli.list; this.index = cli.index; }
}
// vim: tw=100:sw=2
