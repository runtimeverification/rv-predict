package rvpredict.util;

import java.io.File;
import java.util.HashSet;
import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.SharedVarHandler;
import rvpredict.prediction.io.TraceReader;

public class Counter {

  public static void main(String[] args) {
    try {
      TraceReader tr = new TraceReader(args[0] + File.separator + "trace.log");
      HashSet<String> threads = new HashSet<String>();
      Event e = tr.next();
      while (e != null){
        threads.add(e.thread.toString());
        e = tr.next();
      }
      System.out.println(tr.pos + " events in " + threads.size() + " threads.");
      System.out.println("Found " + SharedVarHandler.readShared().size() + " shared variables.");
      System.out.println("Checked " + SharedVarHandler.readToCheck().size() + " shared variables.");
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

}
// vim: tw=100:sw=2
