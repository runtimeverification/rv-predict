package rvpredict;

import rvpredict.logging.Protos;
import rvpredict.traces.ForwardTrace;

class MOPVectorClocker extends VectorClocker<MOPEvent> {
  MOPVectorClocker(ForwardTrace t) { super(t); }
  // Normal reads/writes must have been included because something is data dependent on them thus we
  // need to preserver their ordering. However for important accesses (i.e. those to the variable we
  // are currently checking) preserving their ordering would obscure all races (this is sound I
  // suppose), so we don't. This might present an opening for some unsoundness if an access would be
  // included both from importance and data dependence.
  @Override protected MOPEvent handleAccess(final Protos.Event e) {
    return unimportantAccess(e);
  }
  @Override protected MOPEvent handleMOPEvent(final Protos.Event e) {
    return new MOPEvent(e,threadClocks.get(e.getThreadId()));
  }
  @Override protected MOPEvent handleLockUnlock(final Protos.Event e) {
    return new MOPEvent(e,threadClocks.get(e.getThreadId()));
  }
}
// vim: tw=100:sw=2
