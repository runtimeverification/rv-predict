package rvpredict.prediction.race;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.Stage;
import rvpredict.prediction.generic.SharedVarHandler;

import rvpredict.prediction.PredictorOptions;

import java.util.ArrayList;
import java.util.HashSet;

public class RaceMainStage extends Stage {

  ArrayList<Stage> preStages;
  ArrayList<Stage> stages;

  public RaceMainStage() throws PredictorException{
    super("analyzing for race");
    preStages = new ArrayList<Stage>();
    stages = new ArrayList<Stage>();
    PredictorOptions options = PredictorOptions.v();
    if (!options.manual_list)
      preStages.add(new SharedVarHandler());
    if (options.do_slice) {
      stages.add(new RaceSlicingStage());
      needSoot = true;
    }
    if (options.do_vc || options.do_vc2) {
      stages.add(new RaceVCStage());
    }
    if (options.do_detect) {
      stages.add(new RaceCheckingStage());
    }
  }

  @Override
    public void process() throws PredictorException {
    try {
      for (Stage stage:preStages){
        stage.go();
      }
      HashSet toCheck = SharedVarHandler.readToCheck();
      int size = toCheck.size();
      if (PredictorOptions.v().pipeline_mode) {
        int count = 1;
        for (Object loc:toCheck){
          if (PredictorOptions.v().verbose_level >= 2)
            System.out.println("Handling shared variable #" + count + "/" + size);
          for (Stage stage:stages){
            stage.setId((String)loc);
            stage.go();
          }
          if (PredictorOptions.v().verbose_level >= 2)
            System.out.println("Handling shared variable #" + count + "/" + size + " done.");
          count ++;
        }
      } else {
        for (Stage stage:stages){
          int count = 1;
          for (Object loc:toCheck){
            if (PredictorOptions.v().verbose_level >= 2)
              System.out.println("Handling shared variable #" + count + "/" + size);
            stage.setId((String)loc);
            stage.go();
            if (PredictorOptions.v().verbose_level >= 2)
              System.out.println("Handling shared variable #" + count + "/" + size + " done.");
            count ++;
          }
        }

      }
    } catch (Exception e){
      throw PredictorException.report("RaceMainStage", e);
    }
  }

}
// vim: tw=100:sw=2
