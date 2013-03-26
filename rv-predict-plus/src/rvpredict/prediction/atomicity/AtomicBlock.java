package rvpredict.prediction.atomicity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import rvpredict.PredictorException;
import rvpredict.prediction.PredictorOptions;
import rvpredict.util.Configure;

public class AtomicBlock {
  HashSet<String> locations = new HashSet<String>();
  String thread;
  long start;
  long end;
  int count; // count embedded blocks

  AtomicBlock() {
    start = -1;
    end = -1;
    count = -1;
  }

  AtomicBlock(String str) throws PredictorException {
    int i = str.indexOf(":");
    if (i < 0)
      throw new PredictorException("Wrong format of the atomic block!");
    thread = str.substring(0, i);
    str = str.substring(i + 1);
    String[] ss = str.split(",");
    if (ss.length != 2)
      throw new PredictorException("Wrong format of the atomic block!");
    start = Long.valueOf(ss[0].trim());
    end = Long.valueOf(ss[1].trim());
  }

  void setThread(String t) {
    thread = t;
  }

  void setStart(long l){
    // only set for the outermost start
    if (start == -1) {
      start = l;
      count = 1;
    } else
      count ++;
  }
  void setEnd(long l){
    if (start > -1) {
      end = l;
      count --;
    }
  }

  // this is needed, because slicer works in a backward manner
  void adjust(long total){
    start = total - start;
    end = total - end;
  }

  boolean isDone(){
    return count == 0;
  }
  void addLocation(String loc){
    if (start > -1)
      locations.add(loc);
  }
  public String toString(){
    return thread + ":" + start + ", " + end;
  }

  boolean within(long ln){
    return (ln <= start) && (ln >= end);
  }

  // ---------------------------------------------------- for io ----------------------------------------

  public static HashMap<HashSet<String>, ArrayList<AtomicBlock>> readAtomicBlocks() throws Exception{
    HashMap<HashSet<String>, ArrayList<AtomicBlock>> atomicBlocks = new HashMap<HashSet<String>, ArrayList<AtomicBlock>>();
    BufferedReader reader = new BufferedReader(new FileReader(PredictorOptions.v().work_dir + File.separator + Configure.getString("AtomicBlockFile")  + ".rvpf"));
    String aline = reader.readLine();
    HashSet<String> locations = null;
    ArrayList<AtomicBlock> blockList = null;
    while (aline != null){
      if (aline.startsWith("------")) {
        atomicBlocks.put(locations, blockList);
        locations = null;
        blockList = null;
      } else {
        if (locations == null){
          locations = readLocations(aline);
        } else {
          if (blockList == null)
            blockList = new ArrayList<AtomicBlock>();
          blockList.add(new AtomicBlock(aline));
        }
      }
      aline = reader.readLine();
    }
    return atomicBlocks;
  }
  static HashSet<String> readLocations (String str) throws PredictorException {
    if (! str.startsWith("[") && ! str.endsWith("]"))
      throw new PredictorException("Wrong format of the atomic variable set!");
    str = str.substring(1, str.length() - 1);
    String[] ss = str.split(",");
    HashSet<String> set = new HashSet<String>();
    for (String s:ss)
      set.add(s.trim());
    return set;
  }

}
// vim: tw=100:sw=2
