package rvpredict;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

interface ThreadListIterator<E> {
  public List<E> list(int i);
  public Set<Integer> threads();
  public Collection<Integer> newThreads();
  public void clearNewThreads();
}
// vim: tw=100:sw=2
