package rvpredict;

import java.util.EnumMap;
import java.util.Map;

import rvpredict.util.ActuallyCloneable;

public final class MacerMonitor implements Monitor, ActuallyCloneable<MacerMonitor>,
java.io.Serializable {
  static final long serialVersionUID = 0L;

  public static enum Event { read, creatorRead, write, creatorWrite }
  private State state;
  private static final State virgin = new State();
  private static final State exclusive = new State();
  private static final State exclusiveModified = new State();
  private static final State shared = new State();
  private static final State sharedModified = new State();
  private static final class State extends EnumMap<Event,State> implements java.io.Serializable { 
    static final long serialVersionUID = 0L;
    State() { super(Event.class); }
  }
  static {
    virgin.put(Event.read,exclusive);
    virgin.put(Event.creatorRead,exclusive);
    virgin.put(Event.write,exclusiveModified);
    virgin.put(Event.creatorWrite,exclusiveModified);
    exclusive.put(Event.read,shared);
    exclusive.put(Event.creatorRead,exclusive);
    exclusive.put(Event.write,sharedModified);
    exclusive.put(Event.creatorWrite,exclusiveModified);
    exclusiveModified.put(Event.read,sharedModified);
    exclusiveModified.put(Event.creatorRead,exclusiveModified);
    exclusiveModified.put(Event.write,sharedModified);
    exclusiveModified.put(Event.creatorWrite,exclusiveModified);
    shared.put(Event.read,shared);
    shared.put(Event.creatorRead,shared);
    shared.put(Event.write,sharedModified);
    shared.put(Event.creatorWrite,sharedModified);
    sharedModified.put(Event.read,sharedModified);
    sharedModified.put(Event.creatorRead,sharedModified);
    sharedModified.put(Event.write,sharedModified);
    sharedModified.put(Event.creatorWrite,sharedModified);
  }

  MacerMonitor(final MacerMonitor m) {
    this.state = m.state;
  }

  public MacerMonitor() {
    state = virgin;
  }
  @Override public void process(final String s) {
    process(Event.valueOf(s));
  }
  public void process(final Event e) {
    state = state.get(e);
  }
  @Override public boolean isCoreachable() {
    return true;
  }
  @Override public Category getCategory() {
    if (state == sharedModified)
      return Category.Match;
    else
      return Category.Unknown;
  }

  @Override public MacerMonitor clone() {
    return new MacerMonitor(this);
  }

  @Override public String toString() {
    if (state == virgin)
      return "virgin";
    if (state == exclusive)
      return "exclusive";
    if (state == exclusiveModified)
      return "exclusiveModified";
    if (state == shared)
      return "shared";
    if (state == sharedModified)
      return "sharedModified";
    assert false: "Unreachable";
    return null;
  }
}
// vim: tw=100:sw=2
