package rvpredict;

import rvpredict.traces.ForwardTrace;
import rvpredict.logging.Protos;
import static rvpredict.util.Util.disjoint;
import rvpredict.util.Util;
import rvpredict.util.ActuallyCloneable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.lang.Iterable;
import java.util.concurrent.Callable;


// After Vector Clocking every access will have a vector clock describing when it occurred and a set
// of locks that were held by its owning thread when it occurred. We report a race when there are two
// distinct accesses s.t. their vector clocks are incomparable, they are not protected by a shared
// lock, and at least one is a write.
//
// Since merely reporting that there is a race on a particularly variable is not too useful, we need
// to find the call stack for both accesses once we find a race. This can be done either by storing
// with each access its call stack. Alternatively, we could record its position within the trace and
// replay it to determine the call stack after a race is found. To the extent that most variables
// won't have races the latter option might be better.
//
// There are a few observations that can be used to improve this phase:
// 1) Since we only are looking for pairs of accesses (and there can never be a race within a single
//    thread) we only need to examine accesses from two threads at a time.
// 2) Accesses are totally ordered within one thread and our temporal ordering is a partial order,
//    thus for an event e_1 in thread t_1 we only need to search for races in thread t_2 among
//    {e: e_2 <= e <= e_3} s.t. e_2 < e_1 < e_3 and e_2 and e_3 are events in t_2.

class RaceDetector implements Callable<Boolean> {
  private ThreadListIterator<Access> tli;

  final private Iterator<Access> trace;
  RaceDetector(Iterator<Access> t) {
    trace = t;
  }

  // First stuff all events into array lists representing their events, then do the needed search
  @Override public Boolean call() {
    tli = new ZipListThreadListIterator<Access>(trace);
    return search();
  }

  private static void report(Access a1, Access a2) {
    String fst = reportFMT(a1);
    String snd = reportFMT(a2);

    StringBuffer header = new StringBuffer("/--- Race found on " + a1.var + " ");
    StringBuffer fstOut = new StringBuffer("|  " + fst);
    StringBuffer sndOut = new StringBuffer("|  " + snd);
    StringBuffer tail   = new StringBuffer("\\------------------");

    // The length of the longest line of output. I really don't know why max isn't var-arg...
    int maxlen = java.lang.Math.max(java.lang.Math.max(3+fst.length(), 3+snd.length()), header.length());

    // Form the strings to output. Magic number 3 because that is the length of the prefix ("|  ")
    for (int i = 0; i <= maxlen; i++) {
      if (header.length() < i)  header.append("-");
      if (tail.length()   < i)  tail.append("-");
      if (fstOut.length() < i)  fstOut.append(" ");
      if (sndOut.length() < i)  sndOut.append(" ");
    }

    synchronized (System.out) {
      System.out.println("");
      System.out.println(header + "---\\");
      System.out.println(fstOut + "   |");
      System.out.println(sndOut + "   |");
      System.out.println(tail   + "---/");
      System.out.println("");
    }
  }

  private static String reportFMT(final Access a) {
    return ((a.isRead())?"Read  at ":"Write at ") + a.location.getClassName() + ":" + Util.extractLineNumber(a.location);
  }

  private static boolean isViolation(Access a1, Access a2) {
    return (a1.isWrite() || a2.isWrite()) && disjoint(a1.lockset(),a2.lockset());
  }

  // I wish ListIterator was Cloneable
  private class LIWrapper<T> implements ListIterator<T>, ActuallyCloneable<LIWrapper<T>>, Iterable<T> {
    @Override public void add(final T t) { assert false : "Unimplemented"; }
    @Override public void set(final T t) { assert false : "Unimplemented"; }
    @Override public void remove() { assert false : "Unimplemented"; }
    @Override public int nextIndex() { return it.nextIndex(); }
    @Override public int previousIndex() { return it.previousIndex(); }
    @Override public T next() { return it.next(); }
    @Override public T previous() { return it.previous(); }
    @Override public boolean hasNext() { return it.hasNext(); }
    @Override public boolean hasPrevious() { return it.hasPrevious(); }
    @Override public LIWrapper<T> clone() { return new LIWrapper<T>(this); }
    @Override public Iterator<T> iterator() { return this; }
    private final List<T> list;
    private final ListIterator<T> it;
    LIWrapper(final List<T> l) { list = l; it = l.listIterator(); }
    LIWrapper(final LIWrapper<T> input) { this.list = input.list; this.it = list.listIterator(input.it.nextIndex()); }
  }

  private List<LIWrapper<Access>> getGreaterIterators(final int thread) {
    List<LIWrapper<Access>> ret = new ArrayList<LIWrapper<Access>>();
    for (final int t : tli.threads())
      if (thread < t)
        ret.add(new LIWrapper<Access>(tli.list(t)));
    return ret;
  }

  private Boolean search() {
    for (final int thread : tli.threads()) {
      final List<LIWrapper<Access>> others = getGreaterIterators(thread);
      if (!others.isEmpty()) {
        for (final Access a : tli.list(thread)) {
          ListIterator<LIWrapper<Access>> othersIt = others.listIterator();
          while (othersIt.hasNext()) {
            final LIWrapper<Access> cur = othersIt.next();
            here: while (cur.hasNext()) {
                    switch (cur.next().relativeTime(a)) {
                      case EarlierThan:
                        break;
                      case LaterThan:
                      case Concurrent:
                        cur.previous();
                        othersIt.set(cur.clone());
                        break here;
                    }
            }
            here: for (final Access oa : cur) {
                    switch (a.relativeTime(oa)) {
                      case EarlierThan:
                        break here;
                      case Concurrent:
                        if (isViolation(a,oa)) {
                          report(a,oa);
                          return true;
                        } else
                          break;
                      case LaterThan:
                        assert false : "Unreachable";
                        break;
                    }
            }
          }
        }
      }
    }
    return false;
  }
}

// vim: tw=100:sw=2
