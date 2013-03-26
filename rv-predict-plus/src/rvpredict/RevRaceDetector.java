package rvpredict;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class RevRaceDetector extends RevDetector<Access,RaceSearchState> {
  RevRaceDetector(final Iterator<Access> it) { super(it); }
  private final List<AbstractCLIterator<Access,SuperList<Access>>> threads = new ArrayList<AbstractCLIterator<Access,SuperList<Access>>>();
  @Override public List<RaceSearchState> addThread(final AbstractCLIterator<Access,SuperList<Access>> i1) {
    List<RaceSearchState> ret = new ArrayList<RaceSearchState>();
    for (final AbstractCLIterator<Access,SuperList<Access>> i2 : threads) {
      ret.add(new RaceSearchState(i1.clone(),i2.clone()));
    }
    threads.add(i1.clone());
    return ret;
  }
}
// vim: tw=100:sw=2
