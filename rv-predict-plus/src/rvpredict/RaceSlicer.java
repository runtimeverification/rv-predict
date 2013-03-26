package rvpredict;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;
import java.util.Iterator;

class RaceSlicer extends RealSlicer {
  private final Protos.Variable var;
  RaceSlicer(final Iterator<Protos.Event> t, final Protos.Variable v) {
    super(t);
    var = v;
  }

  @Override protected void markImportant() {
    if (e.getType() == Protos.Event.Type.Access) {
      if (var.equals(e.getVariable())) {
        //events are immutable, so mergeFrom is necessary to get the original value of e
        e = Protos.Event.newBuilder().mergeFrom(e).setIsImportant(true).build();
      }
    }
  }
}

// vim: tw=100:sw=2
