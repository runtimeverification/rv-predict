package rvpredict.logging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import db.DBEngine;
import db.DBEngineParallel;

public final class RecordRT {

//	static HashMap<Long,HashMap<Object,Stack<Object>>>
//		threadReEntrantLockMap = new HashMap<Long,HashMap<Object,Stack<Object>>>();
	final static boolean doTraceFilter = false;
	final static boolean doInterleave = false;
	final static boolean doLogInParallel = false;
	
	static HashMap<Long,HashSet<Integer>> threadlocalWriteIDSet;
	static HashMap<Long,HashSet<Integer>> threadlocalReadIDSet;

	//can be computed during offline analysis
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	final static String MAIN_NAME = "0";
//	private static boolean isReEntrant(long tid, String addr, boolean isLock)
//	{
//		HashMap<Object,Stack<Object>> lockStackMap = threadReEntrantLockMap.get(tid);
//		if(lockStackMap==null)
//		{
//			lockStackMap = new HashMap<Object,Stack<Object>>();
//			threadReEntrantLockMap.put(tid, lockStackMap);
//		}
//		Stack<Object> stack = lockStackMap.get(addr);
//		if(stack==null)
//		{
//			stack = new Stack<Object>();
//			lockStackMap.put(addr, stack);
//		}
//		
//		boolean reentrant = false;
//		if(!stack.isEmpty())
//			reentrant = true;
//		
//		if(isLock)
//			stack.push(new Object());
//		else
//			stack.pop();
//
//		return reentrant;
//	}
//	private static void attachShutDownHook()
//	{
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			   @Override
//			   public void run() {
//				   db.finishLogging();
//			   }
//			  });
//
//	}
	static DBEngine db;
	public static void init(String appname) throws Exception
	{
		long tid = Thread.currentThread().getId();

		if(doLogInParallel)
		{
			db= new DBEngineParallel(appname);//use parallel db engine
			((DBEngineParallel)db).createTraceTable(tid);

		}
		else
		{
			db= new DBEngine(appname);
			db.createTraceTable();
		}	
		
		db.createThreadIdTable();
		
		//db.startAsynchronousLogging();//perform asynchronous logging, hopefully we can improve performance
		
		//attachShutDownHook();
		

		threadTidNameMap = new HashMap<Long,String>();
		db.saveThreadTidNameToDB(tid, MAIN_NAME);
		
		threadTidNameMap.put(tid, MAIN_NAME);

		threadTidIndexMap = new HashMap<Long,Integer>();
		threadTidIndexMap.put(tid, 1);
		
		if(doTraceFilter)
		{
			threadlocalReadIDSet = new HashMap<Long,HashSet<Integer>>();
			threadlocalWriteIDSet = new HashMap<Long,HashSet<Integer>>();
		}
	}
	private static void clearThreadLocalRWIDSet(long tid)
	{
		HashSet<Integer> set = threadlocalWriteIDSet.get(tid);
		if(set==null)
		{
			set = new HashSet<Integer>();
			threadlocalWriteIDSet.put(tid, set);
		}
		threadlocalWriteIDSet.clear();
		
		set = threadlocalReadIDSet.get(tid);
		if(set==null)
		{
			set = new HashSet<Integer>();
			threadlocalReadIDSet.put(tid, set);
		}
		threadlocalReadIDSet.clear();
	}
	private static boolean isRedundant(long tid, int ID, boolean write)
	{
		if(write)
		{
			HashSet<Integer> set = threadlocalWriteIDSet.get(tid);
			if(set==null)
			{
				set = new HashSet<Integer>();
				threadlocalWriteIDSet.put(tid, set);
			}
			
			if(set.contains(ID))
				return true;
			else
				set.add(ID);
		}
		else
		{
			HashSet<Integer> set = threadlocalReadIDSet.get(tid);
			if(set==null)
			{
				set = new HashSet<Integer>();
				threadlocalReadIDSet.put(tid, set);
			}
			
			if(set.contains(ID))
				return true;
			else
				set.add(ID);
		}
		
		return false;
	}
  public static  void logBranch(int ID) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[9]);
  }
  public static void logBasicBlock(int ID)
  {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[10]);
  }

  public static  void logWait(int ID,final Object o) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[5]);
	 
  }

  public static  void logNotify(int ID,final Object o) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[6]);

  }

  public static  void logStaticSyncLock(int ID, int SID) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  String addr = ""+SID;
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  
	  db.saveEventToDB(tid, ID, addr, "", db.tracetypetable[3]);

  }

  public static  void logStaticSyncUnlock(int ID, int SID) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+SID, "", db.tracetypetable[4]);
  }

  public static  void logLock(int ID, final Object lock) {
	  
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[3]);
  }

  public static  void logUnlock(int ID,final Object lock) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[4]);

 }

  
  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, o==null?SID+"":System.identityHashCode(o)+"."+SID, isPrim(v)?v+"":System.identityHashCode(v)+"", write?db.tracetypetable[2]: db.tracetypetable[1]);

  }
  
  public static  void logFieldAcc(int ID, final Object o, int SID, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, o==null?SID+"":System.identityHashCode(o)+"."+SID, "LOOP", write?db.tracetypetable[2]: db.tracetypetable[1]);

  }
  public static void logInitialWrite(int ID, final Object o, int index, final Object v){
	  
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,true))
	  db.saveEventToDB(tid, ID, o==null?index+"":System.identityHashCode(o)+"."+index, isPrim(v)?v+"":System.identityHashCode(v)+"", db.tracetypetable[0]);

  }
  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, System.identityHashCode(o)+"."+index, isPrim(v)?v+"":System.identityHashCode(v)+"", write?db.tracetypetable[2]: db.tracetypetable[1]);
  }
  public static  void logArrayAcc(int ID, final Object o, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, System.identityHashCode(o)+".0", "LOOP", write?db.tracetypetable[2]: db.tracetypetable[1]);
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
	  //it's possible that name is NULL, because this thread is started from library: e.g., AWT-EventQueue-0
	  if(name==null)
	  {
		  name = Thread.currentThread().getName();
		  threadTidIndexMap.put(tid, 1);
		  threadTidNameMap.put(tid,name);
	  }
	  
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