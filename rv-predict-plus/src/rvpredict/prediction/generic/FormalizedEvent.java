package rvpredict.prediction.generic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import rvpredict.util.IString;
import rvpredict.util.Util;

import rvpredict.PredictorException;

public class FormalizedEvent {
  public final static int NORMAL = 10;
  public final static int LOCK = 20;
  public final static int UNLOCK = 30;
  public final static int RUN = 40;
  public final static int START = 50;
  public final static int JOIN = 60;
  public final static int RELEVANT = 70;
  public final static int NOTIFY = 80;
  public final static int WAIT = 90;
  public final static int CLINIT = 100; // a class is used for the first time in the thread
  public final static int ENDCLINIT = 110;
  public final static int SCHEDULE = 120;

  public final IString thread;
  public String ln;
  public String pos;
  //	String pos_method;
  public String loc;
  public int type;
  public boolean isRead;
  public int sourceLn;
  public long id;

  public HashSet<LockInfo> locks;
  public VectorClock vc;

  public FormalizedEvent(String str, long id) throws PredictorException{
    this.id = id;
    String[] args = str.split(Util.seperator);
    if (args.length < 4)
      throw new PredictorException("Wrong formalized event format!");
    thread = IString.get(args[0]);
    ln = args[1];
    if (ln.startsWith("lock")){
      type = LOCK;
      ln = ln.substring("lock".length());
    } else if (ln.startsWith("unlock")){
      type = UNLOCK;
      ln = ln.substring("unlock".length());
    } else if (ln.startsWith("run")){
      type = RUN;
      ln = ln.substring("run".length());
    } else if (ln.startsWith("start")){
      type = START;
      ln = ln.substring("start".length());
    } else if (ln.startsWith("join")){
      type = JOIN;
      ln = ln.substring("join".length());
    } else if (ln.startsWith("rel")){
      type = RELEVANT;
      ln = ln.substring("rel".length());
    } else if (ln.startsWith("notify")){
      type = NOTIFY;
      ln = ln.substring("notify".length());
    } else if (ln.startsWith("wait")){
      type = WAIT;
      ln = ln.substring("wait".length());
    } else if (ln.startsWith("clinit")){
      type = CLINIT;
      ln = ln.substring("clinit".length());
    } else if (ln.startsWith("endcl")){
      type = ENDCLINIT;
      ln = ln.substring("endcl".length());
    } else if (ln.startsWith("sch")){
      type = SCHEDULE;
      ln = ln.substring("sch".length());
    } else
      type = NORMAL;
    pos = args[2];
    //pos_method = args[3];
    loc = args[3];
    if (loc.startsWith("+")) {
      isRead = false;
      loc = loc.substring(1);
    } else
      isRead = true;
    locks = new HashSet<LockInfo>();
    if (args.length > 4){
      vc = new VectorClock(args[4]);
      if (args.length > 5){
        String[] l = args[5].split("\\#");
        for (int i = 0; i < l.length; i++){
          locks.add(new LockInfo(l[i]));
        }
      }
    } else
      vc = null;
  }

  public FormalizedEvent(IString thread, String ln, String pos, String loc, boolean isRead){
    this.thread = thread;
    this.ln = ln;
    this.pos = pos;
    //		this.pos_method = method;
    this.loc = loc;
    this.isRead = isRead;
  }

  public FormalizedEvent(IString thread, String ln, String cls, String method, String loc, boolean isRead){
    this.thread = thread;
    this.ln = ln;
    this.pos = cls + "." + method;
    //		this.pos_method = method;
    this.loc = loc;
    this.isRead = isRead;
  }

  public boolean isLock(){
    return ln.startsWith("lock");
  }

  public boolean isUnlock(){
    return ln.startsWith("unlock");
  }

  public boolean isRun(){
    return ln.startsWith("run");
  }

  public boolean isStart(){
    return ln.startsWith("start");
  }

  public boolean isJoin(){
    return ln.startsWith("join");
  }

  public void addLock(String lock, Integer c){
    locks.add(new LockInfo(lock, c));
  }

  public void addLocks(Iterator<String> it, HashMap<String, Integer> lockCounter){
    while (it.hasNext()){
      String lock = it.next();
      int c = lockCounter.get(lock).intValue();
      locks.add(new LockInfo(lock, c));
    }
  }

  // get the locks shared (i.e., have the same lock and lock counter) by two events
  public HashSet<LockInfo> getSharedLocks(FormalizedEvent e){
    HashSet<LockInfo> r = new HashSet<LockInfo>();
    HashMap<String, LockInfo> ownLocks = new HashMap<String, LockInfo>();
    Iterator<LockInfo> it = this.locks.iterator();
    while (it.hasNext()){
      LockInfo lInfo = it.next();
      ownLocks.put(lInfo.lock, lInfo);
    }
    it = e.locks.iterator();
    while (it.hasNext()){
      LockInfo lInfo = it.next();
      LockInfo ownLock = ownLocks.get(lInfo.lock);
      if ((ownLock != null) && (lInfo.counter == ownLock.counter)){
        r.add(lInfo);
      }
    }
    return r;
  }

  public boolean hasCommonLocks(HashSet<LockInfo> locks){
    if ((locks.size() == 0) || (this.locks.size() == 0))
      return false;
    HashSet<String> ownLocks = new HashSet<String>();
    Iterator<LockInfo> it = this.locks.iterator();
    while (it.hasNext()){
      LockInfo lInfo = it.next();
      ownLocks.add(lInfo.lock);
    }
    int s = ownLocks.size();
    it = locks.iterator();
    while (it.hasNext()){
      LockInfo lInfo = it.next();
      ownLocks.remove(lInfo.lock);
    }
    boolean r = (s != ownLocks.size());
    return r;
  }

  public boolean hasCommonLocks(FormalizedEvent e){
    return hasCommonLocks(e.locks);
  }

  public String toString(){
    StringBuffer buf = new StringBuffer();
    buf.append(thread);
    buf.append(Util.seperator);
    switch (type) {
    case LOCK:
      buf.append("lock");
      break;
    case UNLOCK:
      buf.append("unlock");
      break;
    case RUN:
      buf.append("run");
      break;
    case START:
      buf.append("start");
      break;
    case JOIN:
      buf.append("join");
      break;
    case NOTIFY:
      buf.append("notify");
      break;
    case RELEVANT:
      buf.append("rel");
      break;
    case WAIT:
      buf.append("wait");
      break;
    case CLINIT:
      buf.append("clinit");
      break;
    case ENDCLINIT:
      buf.append("encl");
      break;
    case SCHEDULE:
      buf.append("sch");
      break;
    }
    buf.append(ln);
    buf.append(Util.seperator);
    buf.append(pos);
    //		buf.append(MyLogger.seperator);
    //		buf.append(pos_method);
    buf.append(Util.seperator);
    if (!isRead)
      buf.append("+");
    buf.append(loc);
    if (vc != null){
      buf.append(Util.seperator);
      buf.append(vc.toString());
      buf.append(Util.seperator);
      Iterator<LockInfo> it = locks.iterator();
      while (it.hasNext()){
        String lock = it.next().toString();
        buf.append(lock);
        if (it.hasNext())
          buf.append("#");
      }
    }
    return buf.toString();
  }

  public String toXMLString(){
    StringBuffer buf = new StringBuffer();
    buf.append("<event id = \"" + id + "\">\n");
    buf.append("<thread>" + thread + "</thread>\n");
    buf.append("<type>");
    switch (type) {
    case LOCK:
      buf.append("acquire");
      break;
    case UNLOCK:
      buf.append("release");
      break;
    case RUN:
      buf.append("run");
      break;
    case START:
      buf.append("start");
      break;
    case JOIN:
      buf.append("join");
      break;
    case NOTIFY:
      buf.append("notify");
      break;
    case RELEVANT:
      if (!isRead)
        buf.append("write");
      else
        buf.append("read");
      break;
    case WAIT:
      buf.append("wait");
      break;
    case CLINIT:
      buf.append("clinit");
      break;
    case ENDCLINIT:
      buf.append("encl");
      break;
    case SCHEDULE:
      buf.append("sch");
      break;
    }
    buf.append("</type>\n");
    buf.append("<location>" + loc + "</location>");
    buf.append("</event>\n");
    return buf.toString();
  }

  public class LockInfo {
    String lock;
    int counter;
    public LockInfo(String lock, Integer c){
      this.lock = lock;
      counter = c;
    }
    public LockInfo(String str){
      int i = str.indexOf("*");
      lock = str.substring(0, i);
      counter = Integer.valueOf(str.substring(i+1));
    }
    public String toString(){
      return lock + "*" + counter;
    }
  }
}
// vim: tw=100:sw=2
