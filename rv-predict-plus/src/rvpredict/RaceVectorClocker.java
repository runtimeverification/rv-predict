package rvpredict;

import rvpredict.logging.Protos;
import rvpredict.traces.ForwardTrace;

class RaceVectorClocker extends VectorClocker<Access> {
  RaceVectorClocker(ForwardTrace t) { super(t); }
  // Normal reads/writes must have been included because something is data dependent on them thus we
  // need to preserver their ordering. However for important accesses (i.e. those to the variable we
  // are currently checking) preserving their ordering would obscure all races (this is sound I
  // suppose), so we don't. This might present an opening for some unsoundness if an access would be
  // included both from importance and data dependence.
  @Override protected Access handleAccess(final Protos.Event e) {
    if (e.getIsImportant())
      return new Access(e,threadClocks.get(e.getThreadId()).clone());
    else
      return unimportantAccess(e);
  }
  @Override protected Access handleMOPEvent(final Protos.Event e) {
    return unimportantEvent(e);
  }
  @Override protected Access handleLockUnlock(final Protos.Event e) {
    return unimportantEvent(e);
  }
}
// vim: tw=100:sw=2
