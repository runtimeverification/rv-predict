package rvpredict.prediction;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import rvpredict.PredictorException;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.Stage;

import rvpredict.prediction.io.TraceReader;
import rvpredict.util.Configure;

import soot.Body;
import soot.BodyTransformer;

public class PredictorPhase extends BodyTransformer {

  ArrayList<Stage> stages;
  String mainClass;
  String mainMethod;

  public PredictorPhase(ArrayList<Stage> stages) throws Exception {
    this.stages = stages;
    TraceReader ta = new TraceReader(PredictorOptions.v().work_dir + File.separator + Configure.getString("TraceFile") + ".rvpf");
    Event e = ta.next();
    if (e.eventType != Event.BEGIN)
      throw new PredictorException("An BEGIN event is expected at the beginning of the trace file!");
    mainClass = e.cls.toString();
    mainMethod = e.id.toString();
  }

  @Override
    protected void internalTransform(Body body, String phase, Map options) {
    if ((body.getMethod().getDeclaringClass().getName().compareTo(mainClass)!=0) || (body.getMethod().getSubSignature().compareTo(mainMethod)!=0))
      return;
    try {
      for (Stage stage:stages)
        stage.go();
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
}
// vim: tw=100:sw=2
