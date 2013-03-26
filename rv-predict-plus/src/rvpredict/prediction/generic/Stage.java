package rvpredict.prediction.generic;

import rvpredict.PredictorException;
import rvpredict.analysis.MethodAnalyzer;
import rvpredict.prediction.PredictorOptions;

public abstract class Stage {
  protected MethodAnalyzer ma;
  protected String workDir;
  public boolean needSoot = false;
  public String desc;
  public String id;

  public Stage(String n) throws PredictorException {
    ma = MethodAnalyzer.v();
    workDir = PredictorOptions.v().work_dir;
    desc = n;
  }

  public void setId(String n) {
    id = n;
  }

  public void go() throws PredictorException{
    long startTime = 0;
    if (PredictorOptions.v().verbose_level >= 3) {
      startTime = System.currentTimeMillis();
      String msg = id == null? desc : desc + " for " + id;
      System.out.println("Starting " + msg + "...");
    }
    process();
    if (PredictorOptions.v().verbose_level >= 3) {
      long duration = System.currentTimeMillis() - startTime;
      System.out.println(desc + " done in " + duration + " ms.");
    }
  }

  public abstract void process() throws PredictorException;
}
// vim: tw=100:sw=2
