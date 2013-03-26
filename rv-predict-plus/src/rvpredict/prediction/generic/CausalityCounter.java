package rvpredict.prediction.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rvpredict.prediction.PredictorOptions;
import rvpredict.prediction.io.TraceReader;
import rvpredict.util.Util;

public class CausalityCounter {
	
  static int processOneTrace(TraceReader ta) throws Exception {
    int count = 0;
    FormalizedEvent event = ta.nextF();
    HashMap<String, ArrayList<FormalizedEvent>> threadEvents = new HashMap<String, ArrayList<FormalizedEvent>>();  // mapping from threads to events
    HashMap<String, HashMap<String, FormalizedEvent>> thread2RecentEvents = new HashMap<String, HashMap<String, FormalizedEvent>>(); // mapping form threads to the most recent events affected by a thread
    while (event != null){
      HashMap<String, FormalizedEvent> recentEvents = thread2RecentEvents.get(event.thread.toString());
      if (recentEvents == null) {
        recentEvents = new HashMap<String, FormalizedEvent>();
        thread2RecentEvents.put(event.thread.toString(), recentEvents);
      }
      Iterator it = threadEvents.keySet().iterator();
      while (it.hasNext()){
        String t = (String)it.next();
        if (t.compareTo(event.thread.toString()) != 0){
          ArrayList events = threadEvents.get(t);
          FormalizedEvent recentEvent = recentEvents.get(t);
          for (int i = events.size(); i > 0 ; i--){
            FormalizedEvent e = (FormalizedEvent)events.get(i-1);
            if (e == recentEvent)
              break;
            int r = e.vc.compareTo(event.vc);
            if (r < 0) {
              count ++;
              recentEvents.put(t, e);
              break;
            }
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
    return count;
  }
	
  public static void process(String workDir, String name){
    try {
      TraceReader ta;
      ta  = new TraceReader(Util.makeLogFileName(workDir, "VCTraceFile", name));			
      System.out.println("\tSize1: " + processOneTrace(ta));
      ta  = new TraceReader(Util.makeLogFileName(workDir, "VCTraceFile2", name));			
      System.out.println("\tSize2: " + processOneTrace(ta));
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
  public static void main(String[] args) {
    try {
      PredictorOptions.v().work_dir = args[0];
      HashSet toCheck;
      toCheck = SharedVarHandler.readToCheck();
      Iterator it = toCheck.iterator();
      while (it.hasNext()) {
        String name = (String)it.next();
        System.out.println("For " + name + " ");
        process(args[0], name);
      }
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
}
// vim: tw=100:sw=2
