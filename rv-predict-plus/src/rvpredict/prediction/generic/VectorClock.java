package rvpredict.prediction.generic;

import java.util.HashMap;
import java.util.Iterator;

public class VectorClock {
  HashMap<String, Integer> clock;
  public VectorClock(){
    clock = new HashMap<String, Integer>();
  }
  public VectorClock(String str){
    clock = new HashMap<String, Integer>();
    String[] args = str.split(",");
    for (int i = 0; i < args.length; i++){
      String key = args[i];
      i ++;
      Integer v = Integer.valueOf(args[i]);
      clock.put(key, v);
    }
  }
	
  public void increase(String thread){
    Integer i = clock.get(thread);
    if (i == null)
      clock.put(thread, new Integer(1));
    else
      clock.put(thread, new Integer(i.intValue() + 1));
  }
	
  public VectorClock makeCopy(){
    VectorClock vc = new VectorClock();
    vc.merge(this);
    return vc;
  }
	
  public void merge(VectorClock vc){
    Iterator it = vc.clock.keySet().iterator();
    while (it.hasNext()){
      String key = (String) it.next();
      Integer i = vc.clock.get(key);
      Integer old_i = clock.get(key);
      if (old_i == null)
        clock.put(key, i);
      else if (i.intValue() > old_i.intValue())
        clock.put(key, i);
    }
  }
	
  public String toString(){
    StringBuffer buf = new StringBuffer();
    Iterator it = clock.keySet().iterator();
    while (it.hasNext()){
      String key = (String) it.next();
      Integer i = clock.get(key);
      buf.append(key);
      buf.append(",");
      buf.append(i);
      if (it.hasNext())
        buf.append(",");
    }
    return buf.toString();
  }
	
  /*@
   * return: -1, smaller; 1, larger; 0 incomparable or equal
   */
  public int compareTo(VectorClock vc){
    boolean larger = false;
    boolean smaller = false;
    int c = 0;
    Iterator it = vc.clock.keySet().iterator();
    while (it.hasNext()){
      String key = (String) it.next();
      Integer i = vc.clock.get(key);
      Integer old_i = clock.get(key);
      if (old_i == null) {
        smaller = true;
        c ++;
      }
      else if (i.intValue() > old_i.intValue())
        smaller = true;
      else if (i.intValue() < old_i.intValue())
        larger = true;
    }
    if ((vc.clock.size() - c) < clock.size())
      larger = true;
    if (larger && smaller)
      return 0;
    else if (larger)
      return 1;
    else
      return -1;
		
  }
}
// vim: tw=100:sw=2
