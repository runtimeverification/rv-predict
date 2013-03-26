package rvpredict.prediction.race;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.Stage;

public class RaceCheckingStage extends Stage {

  public RaceCheckingStage() throws PredictorException {
    super("checking races");
  }

  @Override
    public void process() throws PredictorException {
    try {
      RaceDetector rd = new RaceDetector(workDir, id);
      rd.process();
    } catch (Exception e){
      throw PredictorException.report("RaceCheckingStage", e);
    }

  }

}
// vim: tw=100:sw=2
