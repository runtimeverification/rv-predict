package rvpredict.prediction.generic;

import rvpredict.PredictorException;
import rvpredict.util.IString;
import rvpredict.util.Util;

import java.util.ArrayList;

public class Event {
  public static final int ENTER = 5;
  public static final int IFENTER = 10;
  public static final int EXENTER = 15;
  public static final int FIELD = 20;
  public static final int ELEMENT = 30;
  public static final int BEGIN = 40;
  public static final int END = 50;
  public static final int LOCK = 60;
  public static final int UNLOCK = 70;
  public static final int CLINIT = 80;
  public static final int POST = 90;
  public static final int MOP = 100;
  public final IString thread;
  public int ln;
  public int eventType;
  public IString cls;
  public String mopevent;
  public ArrayList<String> mopids;

  // id is the hashcode of the object; if it starts with "+", it is a write;
  // if id is !, it is a static field access
  public IString id;
  public String index;
  boolean isRead;

  public Event(String str) throws PredictorException {
    String[] args = str.split(Util.seperator);
    thread = IString.get(args[0]);
    isRead = true; // only used for field and element accesses
    switch(args.length){
      case 0:
      case 1:
        throw new PredictorException("Unknown format of trace event!");
      case 2: // entering block
        if (args[1].startsWith("If")){
          eventType = IFENTER;
          args[1] = args[1].substring(2);
        } else if (args[1].startsWith("post")) {
          eventType = POST;
          args[1] = args[1].substring(4);
        } else
          eventType = ENTER;
        ln = Integer.parseInt(args[1]);
        cls = IString.get("");
        id = IString.get("");
        break;
      case 3: // begin of a method
        eventType = BEGIN;
        ln = -1;
        cls = IString.get(args[1]);
        id = IString.get(args[2]);
        break;
      case 4: // field access
        if (args[1].startsWith("Ex")){
          eventType = EXENTER;
          args[1] = args[1].substring(2);
        } else if (args[1].startsWith("End")){
          eventType = END;
          args[1] = args[1].substring(3);
        } else if (args[1].startsWith("Loc")){
          eventType = LOCK;
          args[1] = args[1].substring(3);
        } else if (args[1].startsWith("Unl")){
          eventType = UNLOCK;
          args[1] = args[1].substring(3);
        }  else if (args[1].startsWith("Clinit")) {
          eventType = CLINIT;
          args[1] = args[1].substring("Clinit".length());
        } else
          eventType = FIELD;
        ln = Integer.parseInt(args[1]);
        cls = IString.get(args[2]);
        id = IString.get(args[3]);
        if (id.toString().startsWith("+")){
          id = IString.get(id.toString().substring(1));
          isRead = false;
        }
        break;
      case 5: // array access
        ln = Integer.parseInt(args[1]);
        cls = IString.get(args[2]);
        id = IString.get(args[3]);
        index = args[4];
        try {
          Long.parseLong(index);
          eventType = ELEMENT;
        } catch (NumberFormatException e){
          eventType = FIELD;
        }

        if (id.toString().startsWith("+")) {
          isRead = false;
          id = IString.get(id.toString().substring(1));
        }
        break;
      default: // MOP Event
        eventType = MOP;
        mopids = new ArrayList<String>();
        mopevent = args[7];
        for (int i = 8; i < args.length; i++)
          mopids.add(args[i]);
        System.err.println(mopids);
        throw new PredictorException("Unknown format of trace event!");
    }
  }

  public Event(IString thread, int ln, int eventType, IString cls, String id, String index, boolean isRead){
    this.thread = thread;
    this.ln = ln;
    this.eventType = eventType;
    this.cls = cls;
    this.id = IString.get(id);
    this.index = index;
    this.isRead = isRead;
  }

  public String toString(){
    StringBuffer buf = new StringBuffer();
    buf.append(thread);
    if (ln > -1) {
      buf.append(Util.seperator);
      switch (eventType) {
        case ENTER: break;
        case IFENTER: buf.append("If"); break;
        case EXENTER: buf.append("Ex"); break;
        case FIELD: break;
        case ELEMENT: break;
        case BEGIN: break;
        case END: buf.append("End"); break;
        case LOCK: buf.append("Loc"); break;
        case UNLOCK: buf.append("Unl"); break;
        case CLINIT: buf.append("Clinit"); break;
        case POST: buf.append("post"); break;
        default:
          (new Exception("Unknown Event Type in toString()")).printStackTrace();
          System.exit(1);
      }
      buf.append(ln);
    }
    if (eventType > IFENTER){
      buf.append(Util.seperator);
      buf.append(cls);
      buf.append(Util.seperator);
      if (!isRead)
        buf.append("+");
      buf.append(id);
      if (((eventType == ELEMENT) || (eventType == FIELD)) && (index != null)){
        buf.append(Util.seperator);
        buf.append(index);
      }

    }
    return buf.toString();
  }

  public void setFieldName(String name){
    if (eventType == FIELD){
      index = name;
    }
  }

  public String getLoc(){
    StringBuffer buf = new StringBuffer();
    if (eventType > IFENTER) {
      buf.append(cls);
      if (id.toString().compareTo("!") != 0)
        buf.append('@');
      buf.append(id);
    }
    return buf.toString();
  }

  public void setWrite(){
    isRead = false;
  }
}
// vim: tw=100:sw=2
