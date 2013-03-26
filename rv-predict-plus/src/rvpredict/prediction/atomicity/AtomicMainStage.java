package rvpredict.prediction.atomicity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.PredictorOptions;

import rvpredict.prediction.generic.Stage;
import rvpredict.prediction.generic.SharedVarHandler;

public class AtomicMainStage extends Stage {

  ArrayList<Stage> preStages;
  ArrayList<AtomicStage> stages;

  public AtomicMainStage() throws PredictorException {
    super("analyzing for atomicity");
    preStages = new ArrayList<Stage>();
    stages = new ArrayList<AtomicStage>();
    PredictorOptions options = PredictorOptions.v();
    preStages.add(new SharedVarHandler());
    if (options.do_find_atomic) {
      preStages.add(new AtomicBlockFinder());
      needSoot = true;
    }
    if (options.do_slice) {
      stages.add(new AtomicSlicingStage());
      needSoot = true;
    }
    if (options.do_merge) {
      stages.add(new AtomicMergingStage());
    }
    if (options.do_vc) {
      stages.add(new AtomicVCStage());
    }
    if (options.do_detect) {
      stages.add(new AtomicityCheckingStage());
    }

  }
  @Override
    public void process() throws PredictorException {
    try {
      for (Stage stage:preStages){
        stage.go();
      }
      HashMap<HashSet<String>, ArrayList<AtomicBlock>> atomicBlocks = AtomicBlock.readAtomicBlocks();
      int size = atomicBlocks.size();
      if (PredictorOptions.v().pipeline_mode) {
        int count = 1;
        for (HashSet<String> locations:atomicBlocks.keySet()){
          System.out.println("Handling atomic blocks #" + count + "/" + size);
          for (AtomicStage stage:stages){
            stage.setId("atomic#" + count);
            stage.setLists(locations, atomicBlocks.get(locations));
            stage.go();
          }
          System.out.println("Handling atomic blocks #" + count + "/" + size + " done.");
          count ++;
        }
      } else {
        for (AtomicStage stage:stages){
          int count = 1;
          for (HashSet<String> locations:atomicBlocks.keySet()){
            System.out.println("Handling atomic blocks #" + count + "/" + size);
            stage.setId("atomic#" + count);
            stage.setLists(locations, atomicBlocks.get(locations));
            stage.go();
            System.out.println("Handling atomic blocks #" + count + "/" + size + " done.");
            count ++;
          }
        }

      }
    } catch (Exception e){
      throw PredictorException.report("AtomicMainStage", e);
    }

  }

}
// vim: tw=100:sw=2
