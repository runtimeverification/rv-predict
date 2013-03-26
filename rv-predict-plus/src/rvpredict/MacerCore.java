package rvpredict;

import rvpredict.logging.Protos;
import rvpredict.util.DiskHash;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MacerCore {
  private static class VarInfo implements java.io.Serializable {
    static final long serialVersionUID = 0L;

    final int creatorThread;
    final MacerMonitor monitor = new MacerMonitor();
    final Set<Integer> lockset;
    VarInfo(final int thread, final Collection<? extends Integer> locks) { this.creatorThread = thread; lockset = new HashSet<Integer>(locks); }
    @Override public String toString() { return creatorThread + " " + lockset + " " + monitor; }
  }
  //SerializableVariable is needed because we don't want to modify the Protos generated code to make
  //it implement Serializable
  private final DiskHash<SerializableVariable,VarInfo> varInfos = new DiskHash<SerializableVariable,VarInfo>();
  private final Set<Protos.Variable> sharedVars = new HashSet<Protos.Variable>();
  public synchronized boolean process(final Protos.Event e) {
    assert e.getType().equals(Protos.Event.Type.Access): "Macer shouldn't see non Access events";
    final Protos.Variable v = e.getVariable();
    final SerializableVariable sv = new SerializableVariable(v);
    if (!sharedVars.contains(v)) {
      VarInfo vi = varInfos.get(sv);
      if(vi == null) {
        vi = new VarInfo(e.getThreadId(),e.getLocksetList());
        varInfos.put(sv,vi);
      }
      if ((vi.creatorThread == e.getThreadId()) && (e.getIsWrite()))
        vi.monitor.process(MacerMonitor.Event.creatorWrite);
      else if ((vi.creatorThread == e.getThreadId()) && (!e.getIsWrite()))
        vi.monitor.process(MacerMonitor.Event.creatorRead);
      else if ((vi.creatorThread != e.getThreadId()) && (e.getIsWrite()))
        vi.monitor.process(MacerMonitor.Event.write);
      else if ((vi.creatorThread != e.getThreadId()) && (!e.getIsWrite()))
        vi.monitor.process(MacerMonitor.Event.read);
      else assert false: "Unreachable";
      vi.lockset.retainAll(e.getLocksetList());
      if (vi.monitor.getCategory().equals(Monitor.Category.Match) && vi.lockset.isEmpty()) {
        sharedVars.add(v);
        return true;
      }
    }
    return false;
  }
}
// vim: tw=100:sw=2
