package rvpredict;

import soot.Scene;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;

class MOPSlicer extends RealSlicer {
  final MOPInstance instance;

  MOPSlicer(final Iterator<Protos.Event> t, final MOPInstance i) {
    super(t);
    instance = i;
  }

  @Override protected void markImportant() {
    if (e.getType() == Protos.Event.Type.MOPEvent) {
      if (instance.compatable(new MOPInstance(e.getMopidsList()))) {
        e = Protos.Event.newBuilder().mergeFrom(e).setIsImportant(true).build();
      }
    }
  }
}
// vim: tw=100:sw=2
