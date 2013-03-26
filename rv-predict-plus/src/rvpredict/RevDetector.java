package rvpredict;

import rvpredict.traces.ForwardTrace;
import rvpredict.logging.Protos;
import static rvpredict.util.Util.disjoint;
import rvpredict.util.Util;
import rvpredict.util.ActuallyCloneable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
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

abstract class RevDetector<IN extends HasThread & Serializable,SS extends SearchState<SS>> implements Callable<Boolean> {
  public static boolean raceFound = false;

  private final SuperLTLI<IN> tli;

  RevDetector(final Iterator<IN> t) {
    tli = new SuperLTLI<IN>(t);
  }

  // First stuff all events into array lists representing their events, then do the needed search
  @Override public Boolean call() {
    return search();
  }

  private Collection<SS> bag = new HashSet<SS>();

  protected abstract Collection<SS> addThread(AbstractCLIterator<IN,SuperList<IN>> it);

  protected boolean restartSearch = false;

  private Boolean search() {
    while (tli.hasNext() || !bag.isEmpty()) {
      if (tli.hasNext())
        tli.next();
      for (final int thread : tli.newThreads()) {
        final Collection<SS> temp = addThread(tli.list(thread).iterator());
        if (restartSearch) {
          restartSearch = false;
          bag.clear();
        }
        bag.addAll(temp);
      }
      tli.clearNewThreads();
      Collection<SS> newbag = new HashSet<SS>();
      for (final SS ss : bag) {
        if (!tli.hasNext()) {
          ss.vcdone();
        }
        if (ss.advanceable()) {
          for (SS nss : ss.advance()) {
            if (nss.foundSomething()) {
              nss.report();
              return true;
            } else if (!nss.done())
              newbag.add(nss);
          }
        } else if (!ss.done()) {
          newbag.add(ss);
        }
      }
      bag = newbag;
    }
    return false;
  }
}

// vim: tw=100:sw=2
