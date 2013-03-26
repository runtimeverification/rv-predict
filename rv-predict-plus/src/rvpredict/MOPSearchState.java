package rvpredict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MOPSearchState implements SearchState<MOPSearchState> {
  @Override public void vcdone() { vcdone = true; }
  private boolean vcdone = false;
  @Override public int hashCode() { return monitor.hashCode() + locksets.hashCode() + threads.hashCode(); }
  @Override public boolean equals(final Object o) {
    if (o == null) return false;
    if (!(o instanceof MOPSearchState)) return false;
    final MOPSearchState m = (MOPSearchState)o;
    return locksets.equals(m.locksets) && monitor.equals(m.monitor) && threads.equals(m.threads);
  }
  @Override public void report() { System.out.println("Match found"); }
  @Override public boolean foundSomething() { return monitor.getCategory().equals(Monitor.Category.Match); }
  @Override public boolean done() { return !monitor.isCoreachable(); }
  @Override public String toString() { return monitor + " " + linearization; }

  // Begin real implementation
  private final Map<CLIterator<MOPEvent>,Set<Integer>> locksets = new HashMap<CLIterator<MOPEvent>,Set<Integer>>();
  private final Monitor monitor;
  private final List<CLIterator<MOPEvent>> threads = new ArrayList<CLIterator<MOPEvent>>();
  private final List<String> linearization;
  MOPSearchState(final Collection<? extends CLIterator<MOPEvent>> c, final Class monitorClass) {
    Monitor temp;
    try {
      temp = (Monitor)monitorClass.newInstance();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); temp = null; }
    monitor = temp;
    for (final CLIterator<MOPEvent> t : c)
      threads.add(t.clone());
    for (final CLIterator<MOPEvent> t : threads)
      locksets.put(t,new HashSet<Integer>());
    linearization = new ArrayList<String>();
  }
  MOPSearchState(final MOPSearchState mss, final CLIterator<MOPEvent> thread, final MOPEvent e) {
    monitor = mss.monitor.clone();
    CLIterator<MOPEvent> currentThread = thread.clone();
    for (final CLIterator<MOPEvent> t : mss.threads) {
      CLIterator<MOPEvent> newThread;
      if (!t.equals(thread))
        newThread = t.clone();
      else
        newThread = currentThread;
      threads.add(newThread);
      locksets.put(newThread, new HashSet<Integer>(mss.locksets.get(t)));
    }
    linearization = new ArrayList<String>(mss.linearization);
    switch (e.type) {
      case MOP:
        linearization.add(0,e.event);
        monitor.process(e.event);
        locksets.get(currentThread).addAll(e.lockset);
        break;
      case Lock:
        assert locksets.get(currentThread).contains(e.objectID) : "Tried to lock a lock this thread holds.";
        locksets.get(currentThread).remove(e.objectID);
        break;
      case Unlock:
        assert !locksets.get(currentThread).contains(e.objectID) : "Tried to unlock a lock this thread didn't hold.";
        locksets.get(currentThread).add(e.objectID);
        break;
      default:
        assert false: "Unimplemented, type was "+e.type;
    }
  }
  @Override public boolean advanceable() {
    boolean ret = false;
    for (final CLIterator<MOPEvent> it : lateThreads())
      ret |= advanceable(it);
    return ret;
  }
  public boolean advanceable(final CLIterator<MOPEvent> it) {
    if (it.hasNext()) {
      final MOPEvent e = it.next(); it.previous();
      switch (e.type) {
        case MOP:
        case Lock:
          return true;
        case Unlock:
          boolean ret = true;
          for (final CLIterator<MOPEvent> thread : threads) {
            ret &= !locksets.get(thread).contains(e.objectID);
          }
          return ret;
      }
    } else return false;
    return false;
  }
  Collection<CLIterator<MOPEvent>> lateThreads() {
    List<CLIterator<MOPEvent>> ret = new ArrayList<CLIterator<MOPEvent>>();
    for (final CLIterator<MOPEvent> i1 : threads) {
      if (i1.hasNext()) {
        boolean includeable = true;
        final MOPEvent e1 = i1.next(); i1.previous();
        for (final CLIterator<MOPEvent> i2 : threads) {
          if (i1 != i2 && i2.hasNext()) {
            final MOPEvent e2 = i2.next(); i2.previous();
            includeable &= e1.relativeTime(e2).equals(TimingOrder.Ordering.Concurrent);
          }
        }
        if (includeable) {
          ret.add(i1);
        }
      }
    }
    return ret;
  }
  @Override public Collection<MOPSearchState> advance() {
    List<MOPSearchState> ret = new ArrayList<MOPSearchState>();
    for (final CLIterator<MOPEvent> thread : lateThreads()) {
      if (advanceable(thread)) {
        ret.add(new MOPSearchState(this,thread,thread.next()));
        thread.previous();
      }
    }
    return ret;
  }
  private void dumpFront() {
    for (final CLIterator<MOPEvent> thread : threads) {
      if (thread.hasNext()) {
        System.out.println(thread.next());
        thread.previous();
      }
    }
  }
}
// vim: tw=100:sw=2
