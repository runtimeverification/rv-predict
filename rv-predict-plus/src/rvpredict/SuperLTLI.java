package rvpredict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;

import java.io.Serializable;

class SuperLTLI<E extends HasThread & Serializable> implements ThreadListIterator<E> {
  private final SortedMap<Integer,SuperList<E>> map = new TreeMap<Integer,SuperList<E>>();
  private final Iterator<E> it;
  private final Collection<Integer> newThreads = new ArrayList<Integer>();
  SuperLTLI(final Iterator<E> inIt) {
    it = inIt;
  }
  public boolean hasNext() { return it.hasNext(); }
  public void next() {
    assert it.hasNext() : "Try to get off iterator";
    E e = it.next();
    assert e != null;
    SuperList<E> l = map.get(e.getThreadID());
    if (l == null) {
      map.put(e.getThreadID(), new SuperList<E>(it));
      newThreads.add(e.getThreadID());
    }
    map.get(e.getThreadID()).add(e);
  }
  @Override public Collection<Integer> newThreads() { return newThreads; }
  @Override public void clearNewThreads() { newThreads.clear(); }
  @Override public Set<Integer> threads() { return map.keySet(); }
  @Override public SuperList<E> list(final int i) { return map.get(i); }
}

// vim: tw=100:sw=2
