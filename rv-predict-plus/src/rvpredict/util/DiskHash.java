package rvpredict.util;

import java.io.File;

import java.util.Properties;
import java.util.Random;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.htree.HTree;

public class DiskHash<KeyT, ValT> { 
  private final static String dbname = "rvpredict" + (new Random()).nextInt();
  private final static int commitRate = 10000;
  private static RecordManager recman;
  private static int commits = 0;
  
  private HTree htree;

  static {
    Properties props = new Properties();
    props.setProperty(RecordManagerOptions.DISABLE_TRANSACTIONS, "TRUE");
    //props.setProperty(RecordManagerOptions.CACHE_SIZE, "1");
    props.setProperty(RecordManagerOptions.CACHE_TYPE, RecordManagerOptions.NORMAL_CACHE); 
    try{
      recman = RecordManagerFactory.createRecordManager(dbname, props);
    } catch (Exception e){
      throw new RuntimeException(e);
    }
    Runtime.getRuntime().addShutdownHook(
           new Thread() { @Override public void run() {
                             try{
                               recman.close(); 
                               (new File(dbname + ".db")).delete();
                               (new File(dbname + ".lg")).delete();
                              } catch(Exception e){
                                throw new RuntimeException(e);
                              }
                           } 
                        });
  }   

  public DiskHash(){
    try {
      htree = HTree.createInstance(recman);
    } catch ( Exception e ) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public ValT get(KeyT k){
    try {
      return (ValT) htree.get(k); 
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public void put(KeyT k, ValT v){
    try {
      htree.put(k,v);
      ++commits;
      if(commits == commitRate){
        commits = 0;
        recman.commit();
      }
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }
 
  public void remove(KeyT k){
    try {
      htree.remove(k);
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args){
    DiskHash<String, String> foo = new DiskHash<String, String>();
  //  java.util.HashMap<String, String> bar = new java.util.HashMap<String, String>();
  //  DiskHash<String, String> bar = new DiskHash<String, String>();
    foo.put("hello", "hello");
    for(int i = 0; i < 1000000; ++i){
     if(i % 50000 == 0) { System.out.println("mem: " + Runtime.getRuntime().freeMemory()); }
      foo.put("hello" + i, "hello" + i);
    }   
    System.out.println(foo.get("hello"));
    System.out.println(foo.get("hello0"));
    System.out.println(foo.get("hello133745"));
    System.out.println(foo.get("hello435113"));
  //  System.out.println(bar.get("hello" + 133745));
  } 
}

