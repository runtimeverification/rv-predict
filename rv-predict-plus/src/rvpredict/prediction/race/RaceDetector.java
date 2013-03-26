package rvpredict.prediction.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import java.io.File;

import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.PredictorOptions;
import rvpredict.util.Util;

import rvpredict.PredictorException;

import rvpredict.prediction.io.ViolationReporter;

public class RaceDetector {
  TraceReader ta;

  public RaceDetector(String workDir, String name) throws Exception {
    PredictorOptions options = PredictorOptions.v();
    String vcfile = options.do_vc2? "VCTraceFile2" : "VCTraceFile";
    ta  = new TraceReader(workDir+File.separator+Util.makeLogFileName(vcfile, name));
  }

  public void process() throws PredictorException{
    try {
      ViolationReporter reporter = new ViolationReporter();
      FormalizedEvent event = ta.nextF();
      HashMap<String, ArrayList<FormalizedEvent>> threadEvents = new HashMap<String, ArrayList<FormalizedEvent>>();
      while (event != null){
        Iterator it = threadEvents.keySet().iterator();
        while (it.hasNext()){
          String t = (String)it.next();
          if (t.compareTo(event.thread.toString()) != 0){
            ArrayList events = threadEvents.get(t);
            for (int i = events.size(); i > 0 ; i--){
              FormalizedEvent e = (FormalizedEvent)events.get(i-1);
              int r = e.vc.compareTo(event.vc);
              if ((r == 0) && (! e.hasCommonLocks(event)) && (!e.isRead || !event.isRead)){
                if (! Util.isInConstructor(e) && ! Util.isInConstructor(event)) {
                  FormalizedEvent[] violation = new FormalizedEvent[2];
                  violation[0] = e;
                  violation[1] = event;
                  String report = reporter.getViolation(violation);
                  if (report != null) {
                    // violation found!
                    System.out.println("------- Race found! --------");
                    System.out.print(report);
                    System.out.println("------- Race end --------");
                  }
                } else {
                  System.err.println("Need to check: a constructor related race found!");
                }
                if (! PredictorOptions.v().get_all_errors)
                  return;
              } else if (r < 0)
                break;
            }
          }
        }

        ArrayList<FormalizedEvent> events = threadEvents.get(event.thread.toString());
        if (events == null){
          events = new ArrayList<FormalizedEvent>();
          threadEvents.put(event.thread.toString(), events);
        }
        events.add(event);
        event = ta.nextF();
      }

    } catch (Exception e){
      throw PredictorException.report("RaceDetector, pos #" + ta.pos, e);
    }
  }

}
// vim: tw=100:sw=2
