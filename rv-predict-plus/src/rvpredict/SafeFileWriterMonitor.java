package rvpredict;

import java.util.EnumMap;
import java.util.Map;

// NB this has already been reversed
public final class SafeFileWriterMonitor implements Monitor {
  @Override public int hashCode() { return System.identityHashCode(this); }
  @Override public boolean equals(final Object o) {
    if (o == null) return false;
    if (!(o instanceof SafeFileWriterMonitor)) return false;
    return state == ((SafeFileWriterMonitor)o).state;
  }
  public static enum Event { open, close, write }
  private State state;
  private static final class State extends EnumMap<Event,State>{ private static final long serialVersionUID = 0;
    State() { super(Event.class); }
  }
  private static final State E = new State();
  private static final State EO = new State();
  private static final State EOC = new State();
  private static final State EC = new State();
  private static final State EOSC = new State();
  static {
    E.put(Event.open,EO);
    E.put(Event.write,EC);
    E.put(Event.close,EC);
    EO.put(Event.open,EOSC);
    EO.put(Event.write,EOC);
    EO.put(Event.close,EC);
    EC.put(Event.open,EO);
    EC.put(Event.write,EC);
    EC.put(Event.close,EOC);
    EOC.put(Event.open,EOSC);
    EOC.put(Event.write,EOC);
    EOC.put(Event.close,EOC);
    EOSC.put(Event.open,EOSC);
    EOSC.put(Event.write,EOC);
    EOSC.put(Event.close,EOC);
  }

  SafeFileWriterMonitor(final SafeFileWriterMonitor s) {
    this.state = s.state;
  }

  public SafeFileWriterMonitor() {
    state = E;
  }
  public void process(final String s) {
    process(Event.valueOf(s));
  }
  private void process(final Event e) {
    state = state.get(e);
  }
  public boolean isCoreachable() {
    return true;
  }
  @Override public Category getCategory() {
    if (state == EOSC)
      return Category.Match;
    else
      return Category.Unknown;
  }

  @Override public SafeFileWriterMonitor clone() {
    return new SafeFileWriterMonitor(this);
  }

  @Override public String toString() {
    if (state == E)
      return "{error}";
    if (state == EO)
      return "{error, open}";
    if (state == EC)
      return "{error, closed}";
    if (state == EOC)
      return "{error, opened, closed}";
    if (state == EOSC)
      return "{error, opened, start, closed}";

    assert false: "Unreachable";
    return null;
  }
}
// vim: tw=100:sw=2
