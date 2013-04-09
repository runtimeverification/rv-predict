package rvpredict.logging;

import java.util.HashMap;

import db.DBEngine;

public final class RecordRT {

	//can be computed during offline analysis
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	final static String MAIN_NAME = "0";
	
	static DBEngine db;
	public static void init(String appname) throws Exception
	{
		db= new DBEngine(appname);
		db.createTraceTable();
		db.createThreadIdTable();
		
		threadTidNameMap = new HashMap<Long,String>();
		long tid = Thread.currentThread().getId();
		db.saveThreadTidNameToDB(tid, MAIN_NAME);
		
		threadTidNameMap.put(tid, MAIN_NAME);

		threadTidIndexMap = new HashMap<Long,Integer>();
		threadTidIndexMap.put(tid, 1);
	}
  public static  void logBranch(int ID) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[9]);
  }
  public static void logBasicBlock(int ID)
  {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[10]);
  }

  public static  void logWait(int ID,final Object o) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+System.identityHashCode(o), "", db.tracetypetable[5]);

  }

  public static  void logNotify(int ID,final Object o) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+System.identityHashCode(o), "", db.tracetypetable[6]);

  }

  public static  void logStaticSyncLock(int ID, int SID) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+SID, "", db.tracetypetable[3]);

  }

  public static  void logStaticSyncUnlock(int ID, int SID) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+SID, "", db.tracetypetable[4]);
  }

  public static  void logLock(int ID, final Object lock) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+System.identityHashCode(lock), "", db.tracetypetable[3]);
  }

  public static  void logUnlock(int ID,final Object lock) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+System.identityHashCode(lock), "", db.tracetypetable[4]);

 }

  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, o==null?SID+"":System.identityHashCode(o)+"."+SID, isPrim(v)?v+"":System.identityHashCode(v)+"", write?db.tracetypetable[2]: db.tracetypetable[1]);

  }

  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, System.identityHashCode(o)+"."+index, isPrim(v)?v+"":System.identityHashCode(v)+"", write?db.tracetypetable[2]: db.tracetypetable[1]);
  }
  private static boolean isPrim(Object o)
  {
	  if(o instanceof Integer
			  || o instanceof Long
			  || o instanceof Byte
			  || o instanceof Boolean
			  || o instanceof Float
			  || o instanceof Double
			  || o instanceof Short
			  || o instanceof Character)
		  return true;
	  
	  return false;
  }

  public static  void logStart(int ID, final Object o) {
	  long tid = Thread.currentThread().getId();
	  Thread t = (Thread) o;
	  long tid_t = t.getId();
	  
	  String name =  threadTidNameMap.get(tid);
	  int index = threadTidIndexMap.get(tid);
	  if(name.equals(MAIN_NAME))
		  name = ""+index;
	  else
		  name = name+"."+index;
	  
	  threadTidNameMap.put(tid_t, name);
	  threadTidIndexMap.put(tid_t, 1);

	  index++;
	  threadTidIndexMap.put(tid, index);

	  db.saveThreadTidNameToDB(tid_t, name);
	  
	  db.saveEventToDB(tid, ID, ""+tid_t, "", db.tracetypetable[7]);
	  

  }
  public static  void logJoin(int ID, final Object o) {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+((Thread) o).getId(), "", db.tracetypetable[8]);

  }

}