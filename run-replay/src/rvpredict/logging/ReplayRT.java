package rvpredict.logging;

import java.util.HashMap;

/**
 * This ReplayRT class contains the utilities for schedule enforcement
 * 
 * @author jeffhuang
 *
 */
public final class ReplayRT {

	//the lock used for enforcing schedule
	final static Object lock = new Object();

	static Object[] schedule;//input schedule loaded from database
	private static int pos;//current position in the schedule
	
	//the following data structure are used to enforce a consistent
	//thread identification across record and replay
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	final static String MAIN_NAME = "0";
	
	/**
	 * load thread id to name map and schedule from database
	 * @param appname
	 * @param s
	 * @throws Exception
	 */
	public static void init(String appname, Object[] s) throws Exception
	{		
		schedule = s;
		
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

  /**
   * before a thread is started, it is associated with a unique identity.
   * @param o
   */
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

  /**
   * Method for enforce the schedule. If it is not the turn of 
   * the current thread to executed the next access by checking the schedule,
   * the current thread is postponed. Thread postponement is achieved using
   * wait/notifyAll. 
   */
  private static void checkOrder()
  {
	  long tid = Thread.currentThread().getId();
	  String name = threadTidNameMap.get(tid);
	  
	  synchronized(lock)
	  {
		//should we stop running or continue if pos is out of bounds?
		  
		  if(pos<schedule.length)
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
					  
					  if(pos>=schedule.length)
						  break;
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
}