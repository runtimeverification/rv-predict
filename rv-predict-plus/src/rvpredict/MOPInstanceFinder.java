package rvpredict;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.lang.Iterable;

import rvpredict.traces.BackwardTrace;

import rvpredict.logging.Protos;

class MOPInstanceFinder implements Iterable<MOPInstance> {
  Set<MOPInstance> instances = new HashSet<MOPInstance>();
  public Iterator<MOPInstance> iterator() {
    return instances.iterator();
  }
  MOPInstanceFinder(BackwardTrace t) {
    for (Protos.Event e : t) {
      if (e.getType() == Protos.Event.Type.MOPEvent) {
        final MOPInstance i = new MOPInstance(e.getMopidsList());
        if (!instances.contains(i)) {
          final Set<MOPInstance> tmp = new HashSet<MOPInstance>();
          for (final MOPInstance i2 : instances) {
            if (i.compatable(i2))
              tmp.add(i.join(i2));
          }
          instances.addAll(tmp);
          instances.add(i);
        }
      }
    }
  }
}
// vim: tw=100:sw=2
