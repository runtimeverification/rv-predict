package rvpredict.prediction.atomicity;

import rvpredict.PredictorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import rvpredict.prediction.PredictorOptions;
import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.prediction.generic.VCCalculator;
import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.io.TraceWriter;

import rvpredict.util.Util;

public class AtomicityChecker {
  String workDir;
  HashMap<HashSet<String>, ArrayList<AtomicBlock>> atomicBlocks = new HashMap<HashSet<String>, ArrayList<AtomicBlock>>();

  public AtomicityChecker(String workDir) throws Exception {
    this.workDir = workDir;
    atomicBlocks = AtomicBlock.readAtomicBlocks();
  }

  public void process() throws PredictorException {
    int count = 0;
    try {
      for (HashSet<String> locations : atomicBlocks.keySet()){
        long startTime;
        long duarution;
        boolean debug = false;
        String loc = "atomic#" + count;
        count ++;

        if (! debug) {
          System.out.println("Generating the sliced trace for " + locations);
          getTraceSlice(locations, loc);
          System.out.println("Generating the sliced trace for " + locations + " done!");

          startTime = System.currentTimeMillis();
          System.out.println("Computing VC for " + loc );
          VCCalculator vcc = new VCCalculator(workDir, loc);
          vcc.process();
          duarution = System.currentTimeMillis() - startTime;
          System.out.println("Computing VC for " + loc + " done in " + duarution + " ms.");
        }
        startTime = System.currentTimeMillis();
        System.out.println("Checking atomic violations for " + loc );
        AtomicSetChecker checker = new AtomicSetChecker(workDir, loc, atomicBlocks.get(locations));
        checker.process();
        duarution = System.currentTimeMillis() - startTime;
        System.out.println("Checking atomic violations " + loc + " done in " + duarution + " ms.");
      }
    } catch (Exception e){
      PredictorException.report("AtomicityChecker", e);
    }
  }

  public static void getTraceSlice(HashSet<String> locations, String fName) throws Exception {
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


  String makeName(HashSet<String> locations){
    String name = "";
    for (String s:locations)
      name += s + "_";
    return name;
  }

  static class ReaderFront {
    TraceReader tr;
    FormalizedEvent event;
  }

}
// vim: tw=100:sw=2
