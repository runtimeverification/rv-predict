package rvpredict.prediction.io;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.prediction.PredictorOptions;

public class ViolationReporter {
  HashSet<String> reportedViolations;
  public ViolationReporter(){
    reportedViolations = new HashSet<String>();
  }

  public String getFullReport(FormalizedEvent[] violation, String vcfile) {
    try {
      return prettyPrint(getCompleteViolation(violation, new ReverseTraceReader("",vcfile)));
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public String prettyPrint(List<FormalizedEvent> report) {
    if (report == null)
      return "";
    StringBuffer sb = new StringBuffer();
    for (FormalizedEvent e : report) {
      sb.append("<event");
      sb.append(" id=" + e.id);
      sb.append(" thread=" + e.thread);
      sb.append(" isRead=");
      if (e.isRead)
        sb.append("true");
      else {
        sb.append("false");
      }
      sb.append(">\n");
      sb.append("<callstack>\n");
      String pos = e.pos.replaceAll("\\=\\>", "=>\n");
      sb.append(pos + "\n");
      sb.append("</callstack>\n");
      sb.append("<location>\n");
      sb.append(e.loc+ "\n");
      sb.append("</location>\n");
      sb.append("</event>\n");
    }
    return sb.toString();
  }

  public List<FormalizedEvent> getCompleteViolation(FormalizedEvent[] violation, ReverseTraceReader ta) throws Exception {
    String loc = getViolatedLoc(violation);
    if (PredictorOptions.v().compress_results && reportedViolations.contains(loc))
      return null;
    reportedViolations.add(loc);
    ArrayList<FormalizedEvent> report = new ArrayList<FormalizedEvent>();
    int i = 0;
    FormalizedEvent target = violation[i];
    //first, output events until the first target is found
    while (true) {
      FormalizedEvent e = ta.prevF();
      report.add(e);
      if (e.id == target.id)
        break;
    }
    i ++;
    ArrayList<FormalizedEvent> blocked = new ArrayList<FormalizedEvent>(); // the list of blocked events when waiting for the next target event
    ArrayList<FormalizedEvent> waiting = new ArrayList<FormalizedEvent>(); // the list of events that should be output after a target event is found
    for (;i < violation.length; i++) {
      target = violation[i];
      while (true) {
        boolean fromWaiting = (waiting.size() > 0);
        FormalizedEvent e;
        if (fromWaiting) {
          e = waiting.get(0);
          waiting.remove(0);
        } else
          e = ta.prevF();
        // try to output events that are in the same thread as the target event first
        // until the target event is found
        if (e.thread.toString().compareTo(target.thread.toString()) == 0) {
          // if the event is not enabled at this moment, output those blocked events until it is enabled
          int last = enable(e, blocked);
          while (last > -1) {
            report.add(blocked.get(0));
            blocked.remove(0);
            last --;
          }
          report.add(e);
          if (e.id == target.id) {
            waiting.addAll(blocked);
            blocked.clear();
            break;
          }
        } else {
          // if the event is another thread, block it now
          blocked.add(e);
        }
      }
    }
    return report;
  }

  // returns the index of the last event that precedes e
  int enable(FormalizedEvent e, ArrayList<FormalizedEvent> events) {
    for (int i = events.size(); i > 0 ; i--) {
      FormalizedEvent event = events.get(i-1);
      if (e.vc.compareTo(event.vc) > 0) { // found a preceding event
        return i - 1;
      }
    }
    return -1;
  }

  public String getViolation(FormalizedEvent[] violation){
    String loc = getViolatedLoc(violation);
    if (PredictorOptions.v().compress_results && reportedViolations.contains(loc))
      return null;
    reportedViolations.add(loc);
    StringBuilder sb = new StringBuilder();
    for (FormalizedEvent e:violation){
      sb.append("*** Thread " + e.thread);
      if (e.isRead)
        sb.append(" reads ");
      else {
        sb.append(" writes ");
      }
      String pos = e.pos.replaceAll("\\=\\>", "=>\n");
      pos = pos.replaceAll("@", " on line number ");
      sb.append(e.loc + " at \n" + pos + ";\n");
    }
    return sb.toString();
  }

  String getViolatedLoc(FormalizedEvent[] violation) {
    String loc = "";
    for (FormalizedEvent e:violation){
      if (! e.isRead)
        loc += "+";
      int i = e.loc.indexOf("@");
      String l = e.loc;
      if (i > -1) {
        int j = l.indexOf(".", i);
        if (j > -1){
          l = l.substring(0, i) + l.substring(j);
        }
      }
      loc = loc + l + ":" + e.pos;
    }
    return loc;
  }
}
// vim: tw=100:sw=2
