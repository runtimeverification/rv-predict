package rvpredict.prediction.generic;
  
import java.io.IOException;
import java.io.File;

import java.util.HashMap;
      
import rvpredict.prediction.io.ReverseTraceReader;
import rvpredict.prediction.io.TraceWriter;
import rvpredict.util.IString;
import rvpredict.util.Util;
import rvpredict.PredictorException;
                
public class VCCalculator {
  ReverseTraceReader tr;
  HashMap<IString, VectorClock> threadClocks;
  HashMap<String, VectorClock> reads;
  HashMap<String, VectorClock> writes;
  HashMap<String, VectorClock> clinits; // for enforcing finishing clinit first
  HashMap<String, VectorClock> waits; // for notify(All)/wait
  HashMap<String, HashMap<String, Integer>> threadLocks;
  HashMap<String, Integer> lockCounter;
  /*
   * Different from normal threads, TimerThread is started by Timer.schedule.
   * However, the Timer object used for scheduling is different from the real TimerThread, 
   * making it difficult to have a precise connection. 
   * So a conservative solution is implemented here:
   * for all Timer objects, only one vector clock is used.
   * This way a TimerThread depends on the latest Timer.schedule. 
   */
  VectorClock timer_vc; 
  TraceWriter tw;
  public VCCalculator(String workDir, String name) throws IOException {
    tr = new ReverseTraceReader(workDir+File.separator,Util.makeLogFileName("SlicedTraceFile", name));
    tw = new TraceWriter(Util.makeLogFileName(workDir+File.separator, "VCTraceFile", name));
    threadClocks = new HashMap<IString, VectorClock>();
    reads = new HashMap<String, VectorClock>();
    writes = new HashMap<String, VectorClock>();
    clinits = new HashMap<String, VectorClock>();
    waits = new HashMap<String, VectorClock>();
    threadLocks = new HashMap<String, HashMap<String, Integer>>();
    lockCounter = new HashMap<String, Integer>();
    timer_vc = new VectorClock();
  }
                            
  public void process() throws PredictorException {
    try {
      FormalizedEvent event = tr.prevF();
      while (event!=null){
        if (threadClocks.get(event.thread) == null){
          // this should happen only for the main thread
          VectorClock t_vc = new VectorClock();
          threadClocks.put(event.thread, t_vc);
          threadLocks.put(event.thread.toString(), new HashMap<String, Integer>());
        }
        switch (event.type){
        case FormalizedEvent.START: 
          {
            VectorClock vc = threadClocks.get(event.thread).makeCopy();
            threadClocks.put(IString.get(event.loc), vc);
            HashMap<String,Integer> locks = new HashMap<String,Integer>();
            threadLocks.put(event.loc, locks);
          }
          break;
        case FormalizedEvent.SCHEDULE: 
          {
            timer_vc.merge(threadClocks.get(event.thread));
          }
          break;
        case FormalizedEvent.JOIN:
          {
            VectorClock vc = threadClocks.get(event.thread);
            vc.merge(threadClocks.get(IString.get(event.loc)));
          }
          break;
        case FormalizedEvent.NOTIFY:
          {
            VectorClock vc = threadClocks.get(event.thread).makeCopy();
            waits.put(event.loc, vc);
          }
          break;
        case FormalizedEvent.WAIT:
          {
            VectorClock vc = waits.get(event.loc);
            if (vc != null){
              VectorClock t_vc = threadClocks.get(event.thread);
              t_vc.merge(vc);
            }
          }
          break;
        case FormalizedEvent.ENDCLINIT:
          {
            VectorClock vc = threadClocks.get(event.thread).makeCopy();
            clinits.put(event.loc, vc);
          }
          break;
        case FormalizedEvent.CLINIT:
          {
            VectorClock vc = clinits.get(event.loc);
            if (vc != null){
              VectorClock t_vc = threadClocks.get(event.thread);
              t_vc.merge(vc);
            }
          }
          break;
        case FormalizedEvent.RUN:
          {
            if (event.thread.toString().startsWith("java.util.TimerThread")){
              // this is a TimerThread and we won't have a start for it
              threadClocks.get(event.thread).merge(timer_vc);
            }
          }
          break;
        case FormalizedEvent.LOCK:
          {
            if (lockCounter.get(event.loc) == null){
              // a new lock found
              lockCounter.put(event.loc, new Integer(1));
            }
            HashMap<String,Integer> locks = threadLocks.get(event.thread.toString());
            Integer count = locks.get(event.loc);
            if (count == null){
              count = new Integer(1);
            } else {
              count = new Integer(count.intValue() + 1);
            }
            locks.put(event.loc, count);
          }
          break;
        case FormalizedEvent.UNLOCK:
          {
            HashMap<String, Integer> locks = threadLocks.get(event.thread.toString());
            Integer count = locks.get(event.loc);
            if (count != null){
              int i = count.intValue() - 1;
              if (i == 0) {
                locks.remove(event.loc);
                // one locked block finished
                int c = lockCounter.get(event.loc).intValue() + 1;
                lockCounter.put(event.loc, new Integer(c));
              } else
                locks.put(event.loc, new Integer(i));					
            }
          }
          break;
        case FormalizedEvent.RELEVANT:
          {
            VectorClock t_vc = threadClocks.get(event.thread);
            t_vc.increase(event.thread.toString());
            event.vc = t_vc;
            event.addLocks(threadLocks.get(event.thread.toString()).keySet().iterator(), lockCounter);
            tw.writeEvent(event);
          }
          break;
        default: // NORMAL
          {
            VectorClock t_vc = threadClocks.get(event.thread);
            VectorClock w_vc = writes.get(event.loc);
            if (w_vc == null){
              w_vc = new VectorClock();
              writes.put(event.loc, w_vc);
            }
            VectorClock r_vc = reads.get(event.loc);
            if (r_vc == null){
              r_vc = new VectorClock();
              reads.put(event.loc, r_vc);
            }
            t_vc.increase(event.thread.toString());
            if (event.isRead){
              t_vc.merge(w_vc);
              r_vc.merge(w_vc);
              r_vc.merge(t_vc);
            } else {
              w_vc.merge(t_vc);
              w_vc.merge(r_vc);
              r_vc.merge(w_vc);
              t_vc.merge(w_vc);
            }
          }
        }
        event = tr.prevF();
      }
      /*tw.close();
      tr.close();*/
    } catch (Exception e){
      throw PredictorException.report("VCCalculator, pos #" + tr.pos, e);
    }
  }	
}
// vim: tw=100:sw=2
