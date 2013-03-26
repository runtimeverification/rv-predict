package rvpredict.prediction.race;

import soot.options.Options;
import rvpredict.PredictorException;
import rvpredict.prediction.generic.VCCalculator;
import rvpredict.prediction.generic.Stage;

import rvpredict.prediction.PredictorOptions;;

public class RaceVCStage extends Stage {

  public RaceVCStage() throws PredictorException {
    super("computing VCs");
  }

  @Override
    public void process() throws PredictorException {
    try {
      VCCalculator vcc = new VCCalculator(workDir, id);
      vcc.process();
    } catch (Exception e){
      throw PredictorException.report("RaceVCStage", e);
    }
  }

}
// vim: tw=100:sw=2
