package rvpredict.prediction.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rvpredict.prediction.PredictorOptions;
import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.generic.FormalizedEvent.LockInfo;
import rvpredict.util.Util;

public class PermutationCounter {
  static HashMap<String, ArrayList<FormalizedEvent>> threadEvents = new HashMap<String, ArrayList<FormalizedEvent>>();  // mapping from threads to events
  static ArrayList<String> threads = new ArrayList<String>();		
  static class CountCut {
    int tid = 0;
    int[] frontier = new int[threads.size()];
    ArrayList<LockInfo> locks = new ArrayList<LockInfo>();
    HashMap<String, Integer> writeBlock = new HashMap<String, Integer>();
    HashMap<String, Integer> readCount = new HashMap<String, Integer>();
		
    CountCut(){	
    }
		
    CountCut(CountCut cut){
      for (int i=0; i<frontier.length; i++){
        frontier[i] = cut.frontier[i];				
      }
      locks.addAll(cut.locks);
      writeBlock.putAll(cut.writeBlock);
      readCount.putAll(cut.readCount);
    }
		
    boolean enabled(int tid){
      ArrayList<FormalizedEvent>  events = threadEvents.get(threads.get(tid));
      if ((events == null) || (frontier[tid] >= events.size()))
        return false;
      FormalizedEvent event = events.get(frontier[tid]); // the next event to add in the chosen thread
      // check if all the events smaller than the event to add have been added into the trace
      for (int i=0; i<frontier.length; i++){
        if (i != tid){
          ArrayList<FormalizedEvent>  q = threadEvents.get(threads.get(i));
          if ((q != null) && (frontier[i] < q.size())){
            FormalizedEvent e = q.get(frontier[i]);
            if (e.vc.compareTo(event.vc) < 0)
              return false;
          }					
        }
      }
      // check if locks overlap
      HashMap<String, LockInfo> eventLocks = new HashMap<String, LockInfo>();
      Iterator<LockInfo> it = event.locks.iterator();
      Integer writeBlockCounter = null;
      Integer readBlockCounter = 0;
      while (it.hasNext()) {
        LockInfo l = it.next();
        if (l.lock.startsWith(":"))
          writeBlockCounter = l.counter;
        else if (l.lock.startsWith("%"))
          readBlockCounter = l.counter;
        else
          eventLocks.put(l.lock, l);
				
      }
      for (int i=0; i<locks.size(); i++){
        LockInfo l = locks.get(i);
        LockInfo l2 = eventLocks.get(l.lock);
        if ((l2!=null) && (l.counter != l2.counter))
          return false;
      }
      if (event.type == FormalizedEvent.RELEVANT) {
        if ((event.isRead) && (writeBlockCounter != null)) {
          Integer w = writeBlock.get(event.loc);
          if ((w != null) && (writeBlockCounter.intValue() != w.intValue()))
            return false;
          if (w == null)
            writeBlock.put(event.loc, writeBlockCounter);
          Integer r = readCount.get(event.loc);
          if (r == null)
            r = 0;
          r = r + 1;
          readCount.put(event.loc, r);
        } else if ((! event.isRead) && (writeBlockCounter != null)) {
          Integer r = readCount.get(event.loc);
          if (r == null)
            r = 0;
          if (readBlockCounter.intValue() != r.intValue())
            return false;
          r = 0;
          readCount.put(event.loc, r);
          writeBlock.put(event.loc, writeBlockCounter);
        }
      }
      return true;
    }
    void updateInfo(int tid){
      ArrayList<FormalizedEvent>  events = threadEvents.get(threads.get(tid));
      FormalizedEvent event = events.get(frontier[tid]); // the next event to add in the chosen thread
      frontier[tid] ++;
      FormalizedEvent e = frontier[tid] < events.size()? events.get(frontier[tid]):null; // the next event in the thread
      // get the locks of the next event
      HashMap<String, LockInfo> eLocks = new HashMap<String, LockInfo>();			
      if (e !=null) {
        Iterator<LockInfo> it = e.locks.iterator();
        while (it.hasNext()) {
          LockInfo l = it.next();
          if ((!l.lock.startsWith(":")) && (! l.lock.startsWith("%")))
            eLocks.put(l.lock, l);
        }
      }
      // get the locks in the event to add
      HashMap<String, LockInfo> eventLocks = new HashMap<String, LockInfo>();
      HashSet<String> toRemove = new HashSet<String>(); // the locks to remove after this event
      Iterator<LockInfo> it = event.locks.iterator();
      while (it.hasNext()) {
        LockInfo l = it.next();
        if (l.lock.startsWith(":") ||  l.lock.startsWith("%"))
          continue;
        eventLocks.put(l.lock, l);
        LockInfo l2 = eLocks.get(l.lock);
        if ((l2 == null) || (l2.counter != l.counter)){
          toRemove.add(l.lock);
        }
      }
			
      // add new locks if any
      for (int i=0; i<locks.size(); i++){
        LockInfo l = locks.get(i);
        eventLocks.remove(l.lock);
      }
      if (eventLocks.size() > 0)
        locks.addAll(eventLocks.values());
      // remove locks if needed
      if (toRemove.size() > 0) {
        it = locks.iterator();
        while (it.hasNext()) {
          if (toRemove.contains(it.next().lock))
            it.remove();
        }
      }
    }
  }
  static int DFS(){
    int count = 0;
    ArrayList<CountCut> trace = new ArrayList<CountCut>();
    CountCut cut = new CountCut();
    trace.add(cut);
    while (trace.size() > 0) {
      cut = trace.get(trace.size()-1);
      boolean flag = false;
      while (cut.tid < threads.size()){
        if (cut.enabled(cut.tid)){
          CountCut newCut = new CountCut(cut);
          newCut.updateInfo(cut.tid);
          cut.tid++;
          trace.add(newCut);
          flag = true;
          break;
        } else {
          cut.tid ++;
        }				
      }
      if (! flag){ // need to back track				
        flag = true;
        // check if it is a complete path
        for (int i = 0 ; i < cut.frontier.length ; i++){
          ArrayList q = (ArrayList)threadEvents.get(threads.get(i));
          if (q == null) continue;
          if (cut.frontier[i] < q.size()){
            flag = false;
            break;
          }
        }
        if (flag) {
          count ++;
          //System.out.println("path " + count + ": \n");
        }
        trace.remove(trace.size()-1);
      }
			
    }
    return count;
  }
  static int processOneTrace(TraceReader ta) throws Exception {
    threadEvents.clear();
    threads.clear();
    FormalizedEvent event = ta.nextF();
    while (event != null){
      ArrayList<FormalizedEvent> events = threadEvents.get(event.thread.toString());
      if (events == null){
        events = new ArrayList<FormalizedEvent>();
        threadEvents.put(event.thread.toString(), events);
        threads.add(event.thread.toString());
      }
      events.add(event);
      event = ta.nextF();
    }
    return DFS();
  }
  public static void process(String workDir, String name){
    try {
      TraceReader ta;
      String f = Util.makeLogFileName(workDir, "VCTraceFile", name);
      ta  = new TraceReader(f);
      System.out.println("\t processing " + f);
      int s1 = processOneTrace(ta);
      n1 += s1;
      System.out.println("\tSize1: " + s1);
      f = Util.makeLogFileName(workDir, "VCTraceFile2", name);
      ta  = new TraceReader(f);			
      System.out.println("\t processing " + f);
      int s2 = processOneTrace(ta);
      n2 += s2;
      System.out.println("\tSize2: " + s2);
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }
	
  static int n1, n2;
	
  public static void main(String[] args) {
    n1 = n2 = 0;
    try {
      PredictorOptions.v().work_dir = args[0];
      //			process(args[0], "account.Account@26954214.amount");
      HashSet toCheck;
      toCheck = SharedVarHandler.readToCheck();
      Iterator it = toCheck.iterator();
      while (it.hasNext()) {
        String name = (String)it.next();
        System.out.println("For " + name + " ");
        process(args[0], name);
      }
      System.out.println("Final: " + n1 + " vs " + n2);
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

}
// vim: tw=100:sw=2
