package rvpredict.prediction.atomicity;

import rvpredict.PredictorException;

public class AtomicityCheckingStage extends AtomicStage {

  public AtomicityCheckingStage() throws PredictorException {
    super("checking atomicity");
  }

  @Override
    public void process() throws PredictorException {
    try {
      AtomicSetChecker checker = new AtomicSetChecker(workDir, id, blockList);
      checker.process();
    } catch (Exception e){
      throw PredictorException.report("AtomicityCheckingStage", e);
    }
  }

}
// vim: tw=100:sw=2
