package rvpredict;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static rvpredict.util.Util.disjoint;
import rvpredict.util.Util;

class RaceSearchState implements SearchState<RaceSearchState> {
  @Override public void vcdone() { vcdone = true; }
  private boolean vcdone = false;
  @Override public int hashCode() { return i1.hashCode() + i2.hashCode() + state.hashCode(); }
  @Override public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof RaceSearchState)) return false;
    RaceSearchState ss = (RaceSearchState) o;
    return i1.equals(ss.i1) && i2.equals(ss.i2) && state.equals(ss.state);
  }
  private static enum State {Start, Copied, Done, FoundRace}
  private final AbstractCLIterator<Access,SuperList<Access>> i1;
  private final AbstractCLIterator<Access,SuperList<Access>> i2;
  final Access a;
  Access reportAcc;
  private State state = State.Start;
  @Override public boolean done() { return state.equals(State.Done) || state.equals(State.FoundRace) || (!advanceable() && vcdone); }
  @Override public boolean foundSomething() { return state.equals(State.FoundRace); }
  @Override public boolean advanceable() { return i1.hasNext() || i2.hasNext(); }
  RaceSearchState(final AbstractCLIterator<Access,SuperList<Access>> I1, final AbstractCLIterator<Access,SuperList<Access>> I2) 
  { 
	  assert I1.hasNext(); 
	  i1 = I1; i2 = I2; a = i1.next();
  }
  RaceSearchState(final RaceSearchState rss) { a = rss.a; i1 = rss.i1.clone(); i2 = rss.i2.clone(); }
  private static boolean isViolation(Access a1, Access a2) {
    return (a1.isWrite() || a2.isWrite()) && disjoint(a1.lockset(),a2.lockset());
  }
  @Override public List<RaceSearchState> advance() {
    List<RaceSearchState> ret = new ArrayList<RaceSearchState>();
    while (i2.hasNext() && !state.equals(State.Copied)) {
      final Access a2 = i2.next();
      switch (a.relativeTime(a2)) {
        case EarlierThan:
          break;
        case Concurrent:
        case LaterThan:
          state = State.Copied;
          if (i1.hasNext()) {
            i2.previous();
            ret.add(new RaceSearchState(i1.clone(),i2.clone()));
            i2.next();
          } else if (!vcdone && !i1.hasNext()) {
            ret.add(new RaceSearchState(this));
          }
          break;
      }
    }
    i2.previous();
    while (i2.hasNext() && !done()) {
      final Access a2 = i2.next();
      switch (a.relativeTime(a2)) {
        case EarlierThan:
          break;
        case Concurrent:
          if (isViolation(a,a2)) {
            reportAcc = a2;
            state = State.FoundRace;
            ret.add(this);
          }
          break;
        case LaterThan:
          state = State.Done;
          break;
      }
    }
    if (vcdone && !done())
      state = State.Done;
    if (!done())
      ret.add(this);
    return ret;
  }
  @Override public void report() { report(a,reportAcc); }
  private static void report(Access a1, Access a2) {
    String fst = reportFMT(a1);
    String snd = reportFMT(a2);

    StringBuffer header = new StringBuffer(GUIMain.RED + "/--- Race found on " + a1.var + " ");
    StringBuffer fstOut = new StringBuffer(GUIMain.RED + "|  " + fst);
    StringBuffer sndOut = new StringBuffer(GUIMain.RED + "|  " + snd);
    StringBuffer tail   = new StringBuffer(GUIMain.RED + "\\------------------");

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
}
// vim: tw=100:sw=2
