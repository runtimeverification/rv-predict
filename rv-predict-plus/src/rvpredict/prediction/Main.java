package rvpredict.prediction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.ArrayList;

import soot.G;
import soot.Pack;
import soot.PackManager;
import soot.Transform;

import soot.options.Options;

import rvpredict.prediction.atomicity.AtomicMainStage;
import rvpredict.prediction.race.RaceMainStage;
import rvpredict.PredictorException;

import rvpredict.prediction.generic.Preprocessor;
import rvpredict.prediction.generic.Stage;

public class Main {

  public static void printUsage(){
    System.out.println("Usage: java rvpredict.predictor.Main -cp <ClassPath> -wd <WorkDir> -app [MainClass] [-pre] [-slice] [-vc] [-detect]");
  }

  public static void main(String[] args){

    try {
      // parse the options
      args = PredictorOptions.v().parse(args);

      ArrayList<Stage> mainStages = new ArrayList<Stage>();
      boolean needSoot = false;
      PredictorOptions options = PredictorOptions.v();
      if (options.do_pre){
        needSoot = true;
        mainStages.add(new Preprocessor());
      }
      Stage propStage = null;
      switch (options.prop) {
      case PredictorOptions.CHECK_RACE:
        propStage = new RaceMainStage();
        break;
      case PredictorOptions.CHECK_ATOMIC:
        propStage = new AtomicMainStage();
        break;
      default :
        throw new PredictorException("Not supported property checking!");
      }
      needSoot = needSoot || propStage.needSoot;
      mainStages.add(propStage);
      if (needSoot){
        PrintStream outStream = new PrintStream(new FileOutputStream(options.work_dir + File.separator + "soot-output.txt"));
        G.v().out = outStream;
        Options.v().set_output_format(Options.output_format_J);
        Options.v().set_keep_line_number(true);
        Options.v().set_output_dir(options.work_dir + File.separator + "JimpleFiles");
        /* add a phase to transformer pack by call Pack.add */
        Pack jtp = PackManager.v().getPack("jtp");
        jtp.add(new Transform("jtp.predictor", new PredictorPhase(mainStages)));
        soot.Main.main(args);
      } else {
        for (Stage stage:mainStages)
          stage.go();
      }
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

}
// vim: tw=100:sw=2
