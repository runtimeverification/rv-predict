package rvpredict;

import java.util.Iterator;

import rvpredict.logging.Protos;
import rvpredict.traces.BackwardTrace;

class RevRaceVectorClocker extends RevVectorClocker<Access> {
  RevRaceVectorClocker(final Iterator<Protos.Event> t) { super(t); }
  @Override protected Access handleAccess(final Protos.Event e) {
    if (e.getIsImportant())
      return new Access(e,threadClocks.get(e.getThreadId()).clone());
    else
      return unimportantAccess(e);
  }
}
// vim: tw=100:sw=2
