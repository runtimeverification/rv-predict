package rvpredict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;

import java.io.Serializable;

class DumpingListThreadListIterator<E extends HasThread & Serializable> implements ThreadListIterator<E> {
  private final SortedMap<Integer,List<E>> map = new TreeMap<Integer,List<E>>();
  DumpingListThreadListIterator(final Iterator<? extends E> it) {
    while (it.hasNext()) {
      final E e = it.next();
      if (!map.containsKey(e.getThreadID()))
        map.put(e.getThreadID(), new DumpingList<E>());
      map.get(e.getThreadID()).add(e);
    }
  }
  @Override public Set<Integer> threads() { return map.keySet(); }
  @Override public List<E> list(final int i) { return map.get(i); }
  @Override public Collection<Integer> newThreads() { assert false: "Unimplemented"; return null; }
  @Override public void clearNewThreads() { assert false: "Unimplemented"; }
}

// vim: tw=100:sw=2
