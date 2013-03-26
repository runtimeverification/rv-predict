package rvpredict;

import rvpredict.logging.Protos;
import rvpredict.traces.BackwardTrace;

import java.util.HashSet;
import java.util.Set;

class RevMOPVectorClocker extends RevVectorClocker<MOPEvent> {
  final Set<Integer> passedThreads = new HashSet<Integer>();
  RevMOPVectorClocker(final BackwardTrace t) { super(t); }
  @Override protected MOPEvent handleLockUnlock(final Protos.Event e) {
    passedThreads.add(e.getThreadId());
    return new MOPEvent(e,threadClocks.get(e.getThreadId()));
  }
  @Override protected MOPEvent handleMOPEvent(final Protos.Event e) {
    passedThreads.add(e.getThreadId());
    return new MOPEvent(e,threadClocks.get(e.getThreadId()));
  }
}
// vim: tw=100:sw=2
