package rvpredict.logging;

import java.util.HashMap;
import java.util.Vector;

import db.DBEngine;

public final class ReplayRT {

	final static Object lock = new Object();

	static Object[] schedule;
	private static int pos;
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	final static String MAIN_NAME = "0";
	
	/*
	 * load input + schedule from database
	 */
	
	public static void init(String appname, int id) throws Exception
	{		
		DBEngine db = new DBEngine(appname);
		schedule = db.getSchedule(id);
		if(schedule==null)
		{
			//no schedule to replay, just terminate
			System.exit(0);
		}
		
		pos=0;
		
		threadTidNameMap = new HashMap<Long,String>();
		long tid = Thread.currentThread().getId();
		
		threadTidNameMap.put(tid, MAIN_NAME);

		threadTidIndexMap = new HashMap<Long,Integer>();
		threadTidIndexMap.put(tid, 1);
	}

  public static void logWait() {
	  checkOrder();
  }

  public static  void logNotify() {
	  checkOrder();

  }

  public static  void logStaticSyncLock() {
	  checkOrder();

  }

  public static  void logStaticSyncUnlock() {
	  checkOrder();

  }

  public static  void logLock() {
	  checkOrder();

  }

  public static  void logUnlock() {
	  checkOrder();


 }

  public static  void logFieldAcc() {
	  checkOrder();

  }

  public static  void logArrayAcc() {
	  checkOrder();

  }

  public static  void logStart(Object o) {

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
	  
	  checkOrder();

  }
  public static  void logJoin() {
	  checkOrder();

  }

  private static void checkOrder()
  {
	  long tid = Thread.currentThread().getId();
	  String name = threadTidNameMap.get(tid);
	  
	  synchronized(lock)
	  {
		  if(name.equals(schedule[pos]))
		  {
			  //proceed
			  pos++;
		  }
		  else
		  {
			  //postpone
			  while(!name.equals(schedule[pos]))
			  {
				  try {
					  lock.notifyAll();
					  lock.wait(10);//once all other threads finish, need someone to notify the blocked threads 
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
			  
			  pos++;
			  
		  }
	  }
  }
}