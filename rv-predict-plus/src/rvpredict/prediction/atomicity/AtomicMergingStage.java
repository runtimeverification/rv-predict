package rvpredict.prediction.atomicity;

import java.util.ArrayList;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.PredictorOptions;
import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.io.TraceWriter;
import rvpredict.util.Util;

public class AtomicMergingStage extends AtomicStage {

  public AtomicMergingStage() throws PredictorException{
    super("merging trace slices");
  }

  @Override
    public void process() throws PredictorException {
    try {
      getTraceSlice(locations, id);
    } catch (Exception e){
      throw PredictorException.report("AtomicMergingStage", e);
    }
  }

  public void getTraceSlice(HashSet<String> locations, String fName) throws Exception {
    String name = Util.makeLogFileName(PredictorOptions.v().work_dir, "SlicedTraceFile", fName);
    TraceWriter tw = new TraceWriter(name);
    HashSet<ReaderFront> tReaders = new HashSet<ReaderFront>();
    for (String loc:locations) {
      String path = Util.makeLogFileName(PredictorOptions.v().work_dir, "SlicedTraceFile", loc);
      ReaderFront front = new ReaderFront();
      front.tr = new TraceReader(path);
      front.event = front.tr.nextF();
      tReaders.add(front);
    }
    while (! tReaders.isEmpty()){
      ReaderFront currFront = null;
      ArrayList<ReaderFront> toRemove = new ArrayList<ReaderFront>();
      for (ReaderFront front:tReaders){
        if (currFront == null) {
          currFront = front;
        } else {
          long currLn = Long.valueOf(currFront.event.ln);
          long ln = Long.valueOf(front.event.ln);
          if (currLn == ln){
            // when the line number is equal, it is possible that one is normal (relevant event), but the other is relevant (property event)
            if (currFront.event.type == front.event.type) {
              front.event = front.tr.nextF();
              if (front.event == null)
                toRemove.add(front);
            } else if (front.event.type == FormalizedEvent.NORMAL)
              currFront = front;
          } else if (currLn > ln)
            currFront = front;
        }
      }
      tw.writeEvent(currFront.event);
      currFront.event = currFront.tr.nextF();
      if (currFront.event == null)
        tReaders.remove(currFront);
      tReaders.removeAll(toRemove);
    }

    tw.close();
  }

  class ReaderFront {
    TraceReader tr;
    FormalizedEvent event;
  }

}
// vim: tw=100:sw=2
