package rvpredict;

import rvpredict.logging.Protos;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

final class MOPEvent implements PartialOrder<MOPEvent>, TimingOrder<MOPEvent>, HasThread, Serializable {
  private static final long serialVersionUID = 0;
  enum Type { Lock, Unlock, MOP };
  final Type type;
  final VectorClock vc;
  final String event;
  final Integer threadID;
  final Integer objectID;
  final Set<Integer> lockset = new HashSet<Integer>();
  MOPEvent(final Protos.Event e, final VectorClock vc) {
    switch (e.getType()) {
      case MOPEvent:
        type = Type.MOP;
        event = e.getMopname();
        threadID = e.getThreadId();
        this.vc = vc;
        objectID = 0;
        break;
      case Lock:
        type = Type.Lock;
        objectID = e.getObjectId();
        threadID = e.getThreadId();
        this.vc = vc;
        event = null;
        break;
      case Unlock:
        type = Type.Unlock;
        objectID = e.getObjectId();
        threadID = e.getThreadId();
        this.vc = vc;
        event = null;
        break;
      default:
        assert false: "Unimplemented";
        type = null;
        event = null;
        this.vc = null;
        threadID = 0;
        objectID = 0;
        break;
    }
  }
  public String toString() {
    final StringBuffer ret = new StringBuffer();
    switch (type) {
      case MOP:
        ret.append(type).append(" ")
          .append(event).append(" ")
          .append(vc).append(" ")
          .append(threadID);
        break;
      case Lock:
        ret.append(type).append(" ")
          .append(objectID).append(" ")
          .append(vc).append(" ")
          .append(threadID);
        break;
      case Unlock:
        ret.append(type).append(" ")
          .append(objectID).append(" ")
          .append(vc).append(" ")
          .append(threadID);
        break;
      default:
        assert false: "Unimplemented";
        break;
    }
    return ret.toString();
  }

  @Override public int hashCode() {
    assert false: "Unimplemented";
    return 0;
  }
  @Override public boolean equals(final Object o) {
    assert false: "Unimplemented";
    return false;
  }
  @Override public PartialOrder.Ordering compare(final MOPEvent e) {
    return vc.compare(e.vc);
  }
  @Override public TimingOrder.Ordering relativeTime(final MOPEvent e) {
    return vc.relativeTime(e.vc);
  }
  @Override public int getThreadID() {
    return threadID;
  }
}
// vim: tw=100:sw=2
