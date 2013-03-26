package rvpredict.prediction.atomicity;

import java.util.ArrayList;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.EventPattern;
import rvpredict.prediction.generic.SharedVarHandler;
import rvpredict.prediction.generic.Slicer;

public class AtomicSlicingStage extends AtomicStage {

  HashSet shared;

  public AtomicSlicingStage() throws PredictorException {
    super("slicing the trace for atomicity");
  }

  @Override
    public void process() throws PredictorException {
    try {
      if (shared == null)
        shared = SharedVarHandler.readShared();
      ArrayList<EventPattern> l = new ArrayList<EventPattern>();
      l.add(new EventPattern(id, false));
      l.add(new EventPattern(id, true));
      Slicer slicer = new Slicer(ma, l, workDir, shared, id);
      slicer.process();
    } catch (Exception e){
      throw PredictorException.report("AtomicSlicingStage", e);
    }

  }
}
// vim: tw=100:sw=2
