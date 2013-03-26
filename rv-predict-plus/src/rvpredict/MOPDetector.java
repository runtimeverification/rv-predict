package rvpredict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import java.lang.Cloneable;

import rvpredict.util.ActuallyCloneable;
import rvpredict.util.CounterMap;
import rvpredict.util.CloneableDefaultMap;
import rvpredict.util.DefaultMap;
import rvpredict.util.UncloneableDefaultMap;

class MOPDetector extends Thread {
  final private Iterator<MOPEvent> trace;
  private ThreadListIterator<MOPEvent> tli;
  private final Class monitorClass;
  MOPDetector(Iterator<MOPEvent> t, String classname) {
    trace = t;
    Class monitorClassTemp;
    try {
      monitorClassTemp = Class.forName(classname+"Monitor");
    } catch (Exception e) { e.printStackTrace(); System.exit(1); monitorClassTemp = null;}
    monitorClass = monitorClassTemp;
  }

  public void run() {
    tli = new ArrayListThreadListIterator<MOPEvent>(trace);
    System.out.println(naive());
  }

  private boolean naive() {
    bag = new LinkedList<ISearchState>();
    bag.add(new ISearchState());

    boolean ret = false;

    while (!bag.isEmpty()) {
      ISearchState s = bag.remove();
      ret |= process(s);
    }

    return ret;
  }

  // MOP property detection works in the following manner, we perform a search among linearizations
  // of the partial order returned by the vector clocking step that are consistent with the locking
  // observed in the program. Thus we replay the trace one event at a time branching our search
  // whenever there are events that could happen simultaneously (i.e. incomparable vector clocks).

  // Structure to hold the search state we use a queue to do breadth first searching.
  private Queue<ISearchState> bag;

  // Pointers to events. One per thread. Since we use ArrayLists right now these are just ints
  // Note this tracks which events haven't been added to a thread yet.
  private class Pointers implements ActuallyCloneable<Pointers> {
    private final Map<Integer,Integer> currentSpot = new HashMap<Integer,Integer>();
    @Override public int hashCode() { return currentSpot.hashCode(); }
    @Override public boolean equals(final Object o) {
      if (o == null) return false;
      if (!(o instanceof Pointers)) return false;
      return currentSpot.equals(((Pointers)o).currentSpot);
    }
    Pointers() {
      for (final int thread : tli.threads())
        currentSpot.put(thread,new Integer(0));
    }
    Pointers(final Pointers p) {
      for (final int thread : tli.threads())
        currentSpot.put(thread,new Integer(p.currentSpot.get(thread)));
    }
    @Override public Pointers clone() {
      return new Pointers(this);
    }
    boolean valid(final int thread) {
      return (0 <= currentSpot.get(thread)) && (currentSpot.get(thread) <= tli.list(thread).size());
    }
    boolean valid() {
      boolean ret = currentSpot.keySet().equals(tli.threads());
      for (final int thread : tli.threads())
        ret &= valid(thread);
      return ret;
    }
    boolean hasGet(final int thread) {
      return currentSpot.get(thread) < tli.list(thread).size();
    }
    MOPEvent get(final int thread) {
      return tli.list(thread).get(currentSpot.get(thread));
    }
    Set<MOPEvent> getEventsFlat() {
      Set<MOPEvent> ret = new HashSet<MOPEvent>();
      for (final int thread : tli.threads())
        ret.add(get(thread));
      return ret;
    }
    Map<Integer,MOPEvent> getEvents() {
      Map<Integer,MOPEvent> ret = new HashMap<Integer,MOPEvent>();
      for (final int thread : tli.threads())
        ret.put(thread,get(thread));
      return ret;
    }
    boolean hasNext(final int thread) {
      return currentSpot.get(thread)+1 < tli.list(thread).size();
    }
    void next(final int thread) {
      currentSpot.put(thread,currentSpot.get(thread)+1);
      assert valid(): "Advanced Pointers into invalid state";
    }
  }

  // State for continuing searches
  private class ISearchState {
    private final Pointers point;
    ArrayList<String> linearization;
    Map<Integer,Set<Integer>> locksets;
    Monitor monitor;
    ISearchState() {
      point = new Pointers();
      linearization = new ArrayList<String>();
      locksets = new HashMap<Integer,Set<Integer>>(){private final static long serialVersionUID = 0;{
        for (final int thread : tli.threads())
          put(thread,new HashSet<Integer>());
      }};
      try {
        monitor = (Monitor)monitorClass.newInstance();
      } catch (Exception e) {e.printStackTrace(); System.exit(1);}
    }
    ISearchState(final ISearchState s) {
      this.point = s.point.clone();
      this.linearization = new ArrayList<String>(s.linearization);
      this.monitor = s.monitor.clone();
      this.locksets = new HashMap<Integer,Set<Integer>>(){private final static long serialVersionUID = 0;{
        for (final int thread : tli.threads())
          put(thread,new HashSet<Integer>(s.locksets.get(thread)));
      }};
    }

    // We can only advance on a certain thread when
    // 1) it has events left in its trace
    // 2) the next event is not a lock that conflicts with locks held by some other thread
    boolean advanceable(final int thread) {
      if (point.hasGet(thread)) {
        final MOPEvent e = point.get(thread);
        switch (e.type) {
          case MOP:
          case Unlock:
            return true;
          case Lock:
            boolean ret = true;
            for (final int t : tli.threads())
              ret &= !locksets.get(thread).contains(e.objectID);
            return ret;
          default:
            assert false: "Unimplemented, type was "+e.type;
            return true;
        }
      } else {
        return false;
      }
    }

    boolean advanceable() {
      boolean ret = false;
      for (final int thread : tli.threads())
        ret |= advanceable(thread);
      return ret;
    }

    ISearchState advance(final int thread) {
      assert point.hasGet(thread) : "Tried to get off the end of some trace";
      final MOPEvent e = point.get(thread);
      point.next(thread);
      switch (e.type) {
        case MOP:
          linearization.add(e.event);
          System.out.println(linearization);
          monitor.process(e.event);
          System.out.println(monitor.getCategory());
          break;
        case Lock:
          assert !locksets.get(thread).contains(e.objectID) : "Tried to lock an lock this thread holds.";
          locksets.get(thread).add(e.objectID);
          break;
        case Unlock:
          assert locksets.get(thread).contains(e.objectID) : "Tried to unlock an lock this thread didn't hold.";
          locksets.get(thread).remove(e.objectID);
          break;
        default:
          assert false: "Unimplemented, type was "+e.type;
      }
      return this;
    }
  }

  // find a maximal subset of threads s.t. their times are pairwise incomparable or equal
  private Set<Integer> minimalTimes(Pointers point) {
    Set<Integer> ret = new HashSet<Integer>();
    for (final int t1 : tli.threads()) {
      if (point.hasGet(t1)) {
        boolean includable = true;
        for (final int t2 : tli.threads()) {
          if (t1 != t2 && point.hasGet(t2)) {
            MOPEvent e1 = point.get(t1);
            MOPEvent e2 = point.get(t2);
            assert e1.vc != null : "Why no vc for: "+e1;
            assert e2.vc != null : "Why no vc for: "+e2;
            includable &= (e1.compare(e2).equals(PartialOrder.Ordering.Equal) || e1.compare(e2).equals(PartialOrder.Ordering.Incomparable));
          }
        }
        if (includable)
          ret.add(t1);
      }
    }
    return ret;
  }

  private boolean process(final ISearchState s) {
    for (final int thread : minimalTimes(s.point)) {
      ISearchState newss = new ISearchState(s);
      if (newss.advanceable(thread)) {
        newss.advance(thread);
        if (newss.advanceable())
          bag.add(newss);
      }
    }

    return false;
  }
}
// vim: tw=100:sw=2
