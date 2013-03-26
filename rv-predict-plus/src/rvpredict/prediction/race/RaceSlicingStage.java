package rvpredict.prediction.race;

import java.util.ArrayList;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.EventPattern;
import rvpredict.prediction.generic.SharedVarHandler;
import rvpredict.prediction.generic.Slicer;
import rvpredict.prediction.generic.Stage;

public class RaceSlicingStage extends Stage {

  HashSet shared;

  public RaceSlicingStage() throws PredictorException {
    super("slicing");
  }

  @Override
    public void process() throws PredictorException {
    try {
      if (shared == null)
        shared = SharedVarHandler.readShared();
      ArrayList<EventPattern> l = new ArrayList<EventPattern>();
      l.add(new EventPattern(id, false));
      l.add(new EventPattern(id, true));
      System.out.println(id);
      Slicer slicer = new Slicer(ma, l, workDir, shared, id);
      slicer.process();
    } catch (Exception e){
      throw PredictorException.report("RaceSlicingStage", e);
    }
  }

}
// vim: tw=100:sw=2
