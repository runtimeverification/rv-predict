package rvpredict;

import java.util.EnumMap;
import java.util.Map;

// NB this has already been reversed
public final class UnsafeMapIteratorMonitor implements Monitor {
  @Override public int hashCode() { return System.identityHashCode(this); }
  @Override public boolean equals(final Object o) {
    if (o == null) return false;
    if (!(o instanceof UnsafeMapIteratorMonitor)) return false;
    return state == ((UnsafeMapIteratorMonitor)o).state;
  }
  public static enum Event { createCollection, update, create, use }
  private State state;
  private static final class State extends EnumMap<Event,State>{ private static final long serialVersionUID = 0;
    State() { super(Event.class); }
  }
  private static final State fail = new State();
  private static final State unsafeUse = new State(); // Accept
  private static final State initial = new State();
  private static final State used = new State();
  private static final State updatedUsed = new State();
  private static final State manyUpdatedUsed = new State();
  private static final State manyUsed = new State();
  private static final State created = new State();

  static {
    fail.put(Event.createCollection,fail);
    fail.put(Event.create,fail);
    fail.put(Event.update,fail);
    fail.put(Event.use,fail);

    initial.put(Event.createCollection,fail);
    initial.put(Event.create,fail);
    initial.put(Event.update,fail);
    initial.put(Event.use,used);

    used.put(Event.createCollection,fail);
    used.put(Event.create,fail);
    used.put(Event.update,updatedUsed);
    used.put(Event.use,fail);

    updatedUsed.put(Event.createCollection,fail);
    updatedUsed.put(Event.create,created);
    updatedUsed.put(Event.update,manyUpdatedUsed);
    updatedUsed.put(Event.use,manyUsed);

    manyUpdatedUsed.put(Event.createCollection,fail);
    manyUpdatedUsed.put(Event.create,created);
    manyUpdatedUsed.put(Event.update,manyUpdatedUsed);
    manyUpdatedUsed.put(Event.use,manyUsed);

    manyUsed.put(Event.createCollection,fail);
    manyUsed.put(Event.create,created);
    manyUsed.put(Event.update,fail);
    manyUsed.put(Event.use,manyUsed);

    created.put(Event.createCollection,unsafeUse);
    created.put(Event.create,fail);
    created.put(Event.update,created);
    created.put(Event.use,fail);

    unsafeUse.put(Event.createCollection,fail);
    unsafeUse.put(Event.create,fail);
    unsafeUse.put(Event.update,fail);
    unsafeUse.put(Event.use,fail);
  }

  UnsafeMapIteratorMonitor(final UnsafeMapIteratorMonitor s) {
    this.state = s.state;
  }

  public UnsafeMapIteratorMonitor() {
    state = initial;
  }
  public void process(final String s) {
    process(Event.valueOf(s));
  }
  private void process(final Event e) {
    state = state.get(e);
  }
  public boolean isCoreachable() {
    return state != fail;
  }
  @Override public Category getCategory() {
    // todo: in the future make fail state be in a category that signifies that it cannot ever proceed
    if (state == unsafeUse)
      return Category.Match;
    else // All non-matches are unknown
      return Category.Unknown;
  }

  @Override public UnsafeMapIteratorMonitor clone() {
    return new UnsafeMapIteratorMonitor(this);
  }

  @Override public String toString() {
    return state.toString();
  }
}
// vim: tw=100:sw=2
