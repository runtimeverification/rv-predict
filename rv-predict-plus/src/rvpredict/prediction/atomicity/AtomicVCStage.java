package rvpredict.prediction.atomicity;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.VCCalculator;

public class AtomicVCStage extends AtomicStage {

  public AtomicVCStage() throws PredictorException {
    super("computing VCs");
  }

  @Override
    public void process() throws PredictorException {
    try {
      VCCalculator vcc = new VCCalculator(workDir, id);
      vcc.process();
    } catch (Exception e){
      throw new PredictorException(e.toString());
    }
  }

}
// vim: tw=100:sw=2
