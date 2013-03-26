package rvpredict;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.Iterable;
import java.util.Map;
import java.util.Set;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;

class SharedVarFinder implements Iterable<Protos.Variable> {
  final Set<Protos.Variable> sharedVars = new HashSet<Protos.Variable>();
  final Map<Protos.Variable,Integer> accessToThread = new HashMap<Protos.Variable,Integer>();
  SharedVarFinder(final BackwardTrace trace) {
    for (final Protos.Event e : trace) {
      if (e.getType().equals(Protos.Event.Type.Access)) {
        final Integer ownerThread = accessToThread.get(e.getVariable());
        if (ownerThread == null) {
          accessToThread.put(e.getVariable(),e.getThreadId());
        } else if (ownerThread != e.getThreadId()) {
          sharedVars.add(e.getVariable());
        }
      }
    }
  }
  @Override public Iterator<Protos.Variable> iterator() {
    return sharedVars.iterator();
  }
}

// vim: tw=100:sw=2
