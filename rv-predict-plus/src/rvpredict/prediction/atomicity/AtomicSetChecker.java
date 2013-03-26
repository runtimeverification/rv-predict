package rvpredict.prediction.atomicity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.FormalizedEvent;

import rvpredict.util.Util;

import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.io.ViolationReporter;

public class AtomicSetChecker {
  TraceReader tr;
  HashMap<String, Front> fMap;
  ViolationReporter reporter;
  String vcfile;

  public AtomicSetChecker(String workDir, String name, ArrayList<AtomicBlock> blockList) throws Exception {
    vcfile = Util.makeLogFileName(workDir, "VCTraceFile", name);
    tr  = new TraceReader(vcfile);
    fMap = new HashMap<String, Front>();
    for (AtomicBlock b:blockList) {
      Front front = fMap.get(b.thread);
      if (front == null){
        front = new Front();
        fMap.put(b.thread, front);
      }
      front.blockList.add(b);
    }
    reporter = new ViolationReporter();
  }

  public void process() throws PredictorException{
    try {
      FormalizedEvent event = tr.nextF();
      while (event != null){
        Front front = fMap.get(event.thread.toString());
        if (front == null) {
          front = new Front();
          fMap.put(event.thread.toString(), front);
        }
        front.eventList.add(event);
        front.move();
        boolean flag = false;
        if (front.block != null){
          long ln = Long.valueOf(event.ln);
          if (front.block.within(ln)) {
            front.addEvent(event);
            flag = true;
          } else if (front.block.end > ln) // move out of the atomic block
            front.block = null;
        }
        for (String t:fMap.keySet()){
          if (t.compareTo(event.thread.toString()) != 0){
            Front f = fMap.get(t);
            for (ArrayList<FormalizedEvent> set: f.atomicSetList){
              if (checkNewEventWithBlock(event, set))
                return;
              if (flag){
                //an atomic block is changed
                ArrayList<FormalizedEvent> newSet = front.atomicSetList.get(front.atomicSetList.size()-1);
              }
            }
            if (flag) {
              ArrayList<FormalizedEvent> newSet = front.atomicSetList.get(front.atomicSetList.size()-1);
              for (int i = f.eventList.size(); i > 0; i--){
                FormalizedEvent e = f.eventList.get(i-1);
                FormalizedEvent begin = newSet.get(0);
                if (e.vc.compareTo(begin.vc) < 0)
                  break;
                if (checkNewBlockWithEvent(newSet, e))
                  return;
              }
            }
          }
        }
        event = tr.nextF();
      }
    }  catch (Exception e){
      throw PredictorException.report("AtomicSetChecker, pos #" + tr.pos, e);
    }
  }

  /*
   * The following check functions are implemented according to a POPL'06 paper, "Associating Syncrhonization Constraints with Data in an Object-Oriented Language"
   * There are 11 patterns of atomicity violations to check.
   * Only the pattern id is give in what follows; refer to the paper for concrete explanation
   */


  boolean checkNewEventWithBlock (FormalizedEvent newEvent, ArrayList<FormalizedEvent> set) {
    // suppose the newEvent is at u', check for patterns from 1 to 5
    if (newEvent.isRead){
      // only pattern 3 is possible
      FormalizedEvent write1 = null;
      for (int i = set.size(); i > 0; i--){
        FormalizedEvent e = set.get(i-1);
        if ((newEvent.vc.compareTo(e.vc) > 0) && (write1 == null))
          break;
        else if ((newEvent.loc.compareTo(e.loc) ==0 ) && (!e.isRead)){
          if (write1 == null) // the write on the right found
            write1 = e;
          else if (! protectedByLocks(e, newEvent, write1)){
            FormalizedEvent[] violation = new FormalizedEvent[3];
            violation[0] = e;
            violation[1] = newEvent;
            violation[2] = write1;
            String report = reporter.getFullReport(violation, vcfile);
            if (report != null) {
              // violation found!
              System.out.println("---- Atomicity violation found: ");
              System.out.print(report);
              System.out.println("---- Atomicity violation end ---- ");
            }
          }
        }
      }
    } else {
      // pattern 1,2,4,5 are possible
      FormalizedEvent e1 = null;
      for (int i = set.size(); i > 0; i--){
        FormalizedEvent e = set.get(i-1);
        if ((newEvent.vc.compareTo(e.vc) > 0) && (e1 == null))
          break;
        else if (newEvent.loc.compareTo(e.loc) == 0){
          if (e1 == null) // the event on the right found
            e1 = e;
          else if (! protectedByLocks(e, newEvent, e1)) {
            FormalizedEvent[] violation = new FormalizedEvent[3];
            violation[0] = e;
            violation[1] = newEvent;
            violation[2] = e1;
            String report = reporter.getFullReport(violation, vcfile);
            if (report != null) {
              // violation found!
              System.out.println("---- Atomicity violation found: ");
              System.out.print(report);
              System.out.println("---- Atomicity violation end ---- ");
            }
          }
        }
      }

    }
    return false;
  }

  // check for lock information
  // the order of the arguments should follow the order in the pattern
  boolean protectedByLocks(FormalizedEvent e1, FormalizedEvent eu, FormalizedEvent e2){
    HashSet<FormalizedEvent.LockInfo> sharedLocks = e1.getSharedLocks(e2);
    return eu.hasCommonLocks(sharedLocks);
  }

  boolean checkNewBlockWithEvent (ArrayList<FormalizedEvent> newSet, FormalizedEvent e){
    // suppose the new event is added to u, check for patterns from 1 to 5 against an existing event
    FormalizedEvent newEvent = newSet.get(newSet.size() - 1);
    if ((e.vc.compareTo(newEvent.vc) <= 0) && (e.loc.compareTo(newEvent.loc) == 0)){
      for (int i = 0; i < newSet.size() - 1; i++){
        FormalizedEvent e1 = newSet.get(i);
        if (e.vc.compareTo(e1.vc) < 0)
          break;
        if (e.loc.compareTo(e1.loc) == 0){
          if ((! e.isRead) || (! newEvent.isRead && ! e1.isRead)){
            if (! protectedByLocks(e1, e, newEvent)) {
              FormalizedEvent[] violation = new FormalizedEvent[3];
              violation[0] = e1;
              violation[1] = e;
              violation[2] = newEvent;
              String report = reporter.getFullReport(violation, vcfile);
              if (report != null) {
                // violation found!
                System.out.println("---- Atomicity violation found: ");
                System.out.println(report);
                System.out.println("---- Atomicity violation end ---- ");
              }
            } else
              break;
          }
        }
      }
    }
    return false;
  }

  boolean check(ArrayList<FormalizedEvent> newSet, ArrayList<FormalizedEvent> set){
    return false;
  }

  class Front {
    AtomicBlock block = null;
    ArrayList<AtomicBlock> blockList = new ArrayList<AtomicBlock>();
    ArrayList<ArrayList<FormalizedEvent>> atomicSetList = new ArrayList<ArrayList<FormalizedEvent>>(); // the list of atomic sets of events
    ArrayList<FormalizedEvent> eventList = new ArrayList<FormalizedEvent>(); // contains all the events for a thread

    void addEvent(FormalizedEvent e){
      atomicSetList.get(atomicSetList.size() - 1).add(e);
    }

    // move to the next atomic block if needed
    void move(){
      if ((block == null) && blockList.size() > 0) {
        block = blockList.remove(0);
        atomicSetList.add(new ArrayList<FormalizedEvent>());
      }
    }
  }
}
// vim: tw=100:sw=2
