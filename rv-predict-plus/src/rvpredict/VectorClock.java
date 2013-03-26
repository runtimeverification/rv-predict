package rvpredict;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.Serializable;

import rvpredict.util.ActuallyCloneable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

final public class VectorClock implements ActuallyCloneable<VectorClock>, TimingOrder<VectorClock>, Serializable, Lattice<VectorClock> {
  private static final long serialVersionUID = 0;
  private final ImmutableMap<Integer, Integer> clock;
  @Override public VectorClock clone() { return this; }
  private VectorClock(final VectorClock vc) { this.clock = ImmutableMap.copyOf(vc.clock); }
  private VectorClock(final Map<Integer,Integer> m) { this.clock = ImmutableMap.copyOf(m); }
  public VectorClock() { this.clock = ImmutableMap.of(); }

  public VectorClock increment(final int thread) {
    final Map<Integer,Integer> map = new HashMap<Integer,Integer>(this.clock);
    final Integer i = map.get(thread);
    if (i == null)
      map.put(thread,1);
    else
      map.put(thread,map.get(thread)+1);
    return new VectorClock(map);
  }
  public VectorClock decrement(final int thread) {
    final Map<Integer,Integer> map = new HashMap<Integer,Integer>(this.clock);
    final Integer i = map.get(thread);
    if (i == null)
      map.put(thread,-1);
    else
      map.put(thread,map.get(thread)-1);
    return new VectorClock(map);
  }

  @Override public TimingOrder.Ordering relativeTime(final VectorClock vc) {
    boolean foundSmaller = false;
    boolean foundLarger = false;
    for (final int thread : Sets.union(this.clock.keySet(),vc.clock.keySet())) {
      final Integer Itmp1 = this.clock.get(thread);
      final Integer Itmp2 = vc.clock.get(thread);
      final int i1 = (Itmp1 == null)?0:Itmp1;
      final int i2 = (Itmp2 == null)?0:Itmp2;
      foundSmaller |= i1 < i2;
      foundLarger  |= i1 > i2;
    }
    if (foundLarger == foundSmaller)
      return TimingOrder.Ordering.Concurrent;
    else if (foundSmaller)
      return TimingOrder.Ordering.EarlierThan;
    else // foundLarger
      return TimingOrder.Ordering.LaterThan;
  }

  @Override public PartialOrder.Ordering compare(final VectorClock vc) {
    assert false: "Unimplemented";
    return null;
  }

  @Override public VectorClock join(final VectorClock vc) {
    final Map<Integer,Integer> map = new HashMap<Integer,Integer>(this.clock);
    for (final int thread : Sets.union(this.clock.keySet(),vc.clock.keySet())) {
      final Integer i1 = this.clock.get(thread);
      final Integer i2 = vc.clock.get(thread);
      assert (i1 != null) || (i2 != null);
      if (i1 == null)
        map.put(thread,Math.max(0,i2));
      else if (i2 == null)
        map.put(thread,Math.max(0,i1));
      else
        map.put(thread,Math.max(i1,i2));
    }
    return new VectorClock(map);
  }

  @Override public VectorClock meet(final VectorClock vc) {
    final Map<Integer,Integer> map = new HashMap<Integer,Integer>(this.clock);
    for (final int thread : Sets.union(this.clock.keySet(),vc.clock.keySet())) {
      final Integer i1 = this.clock.get(thread);
      final Integer i2 = vc.clock.get(thread);
      assert (i1 != null) || (i2 != null);
      if (i1 == null)
        map.put(thread,Math.min(0,i2));
      else if (i2 == null)
        map.put(thread,Math.min(0,i1));
      else
        map.put(thread,Math.min(i1,i2));
    }
    return new VectorClock(map);
  }

  @Override public int hashCode() {
    assert false: "Unimplemented";
    return 0;
  }
  @Override public boolean equals(final Object o) {
    assert false: "Unimplemented";
    return false;
  }

  @Override public String toString() { return clock.toString(); }

}
// vim: tw=100:sw=2
