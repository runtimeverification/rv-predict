package rvpredict;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class RevMOPDetector extends RevDetector<MOPEvent,MOPSearchState> {
  private Class monitorClass;
  private final List<AbstractCLIterator<MOPEvent,SuperList<MOPEvent>>> threads = new ArrayList<AbstractCLIterator<MOPEvent,SuperList<MOPEvent>>>();
  RevMOPDetector(final RevMOPVectorClocker mopvc, final String monitorName) {
    super(mopvc);
    try {
      monitorClass = Class.forName(monitorName+"Monitor");
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
  @Override public List<MOPSearchState> addThread(final AbstractCLIterator<MOPEvent,SuperList<MOPEvent>> it) {
    restartSearch = true;
    final List<MOPSearchState> ret = new ArrayList<MOPSearchState>();
    threads.add(it);
    ret.add(new MOPSearchState(threads,monitorClass));
    return ret;
  }
}

// vim: tw=100:sw=2
