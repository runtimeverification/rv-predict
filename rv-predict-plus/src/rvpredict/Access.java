package rvpredict;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.io.Serializable;

import rvpredict.logging.Protos;

public final class Access implements TimingOrder<Access>, HasThread, Serializable {
  public static final long serialVersionUID = 0;
  public final Variable var;
  public final VectorClock vc;
  private final Set<Integer> lockset;
  private final boolean isWrite;
  public final int threadID;
  public final Location location;

  Access(final Protos.Event e, final VectorClock vc) {
    this.var = new Variable(e.getVariable());
    this.vc = vc.clone();
    this.lockset = new HashSet<Integer>(e.getLocksetList());
    this.isWrite = e.getIsWrite();
    this.threadID = e.getThreadId();
    this.location = new Location(e.getLocation());
  }

  public boolean isRead() { return !isWrite; }
  public boolean isWrite() { return isWrite; }
  public Set<Integer> lockset() { return Collections.unmodifiableSet(lockset); }

  public int hashCode() {
    return var.hashCode()+vc.hashCode()+lockset.hashCode()+threadID;
  }

  public boolean equals(final Object o) {
    if ((o == null) || (!(o instanceof Access)))
      return false;
    Access a = (Access)o;
    return (isRead() == a.isRead()) && (threadID == a.threadID) && var.equals(a.var) && vc.equals(a.vc) && lockset.equals(a.lockset) && location.equals(a.location);
  }

  public String toString() {
    return (isRead()?"Read of ":"Write of ") + var.toString() + " at time " + vc + " with locks " + lockset + " at jimple location " + location;
  }

  @Override public PartialOrder.Ordering compare(final Access a) {
    return vc.compare(a.vc);
  }
  @Override public TimingOrder.Ordering relativeTime(final Access a) {
    return vc.relativeTime(a.vc);
  }
  @Override public int getThreadID() { return threadID; }
}
// vim: tw=100:sw=2
